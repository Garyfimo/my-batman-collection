package com.garyfimo.batmancollection.figure.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureAddResult
import com.garyfimo.batmancollection.figure.domain.FigureDraft
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureLookup
import com.garyfimo.batmancollection.figure.domain.FigureRemoveResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.core.domain.FigureType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FirestoreFigureRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context,
) : FigureRepository {

    override fun observe(): Flow<FigureListResult> = callbackFlow {
        // Order by createdAt desc so new adds surface at the top. Figures migrated
        // without a createdAt sort below ones that have it.
        val registration = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(FigureListResult.Failure(error))
                    snapshot != null -> trySend(
                        FigureListResult.Success(snapshot.documents.mapNotNull { it.toFigure() }),
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun get(id: String): FigureLookup = try {
        val doc = firestore.collection(COLLECTION).document(id).get().await()
        when {
            !doc.exists() -> FigureLookup.NotFound
            else -> doc.toFigure()?.let(FigureLookup::Found) ?: FigureLookup.NotFound
        }
    } catch (t: Throwable) {
        FigureLookup.Failure(t)
    }

    override suspend fun add(draft: FigureDraft): FigureAddResult {
        // Reserve a Firestore doc id up front so the Storage path and the Firestore
        // doc share it. Picking Firestore's auto-id instead of a slug-based one keeps
        // the client write path collision-free and side-steps a "does this slug already
        // exist?" round-trip.
        var tempFile: File? = null
        return try {
            val docRef = firestore.collection(COLLECTION).document()
            val id = docRef.id

            // Resize before upload: full-resolution phone photos were ~7 MB each, and the
            // web viewer renders them at ~300 px cards, so 99% of the bytes were wasted.
            // Clamp to MAX_DIM, re-encode as JPEG, serve with a long browser cache.
            tempFile = resizeToJpegFile(Uri.parse(draft.imageUri))
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCacheControl(CACHE_CONTROL)
                .build()

            val imageRef = storage.reference.child("$COLLECTION/$id.jpg")
            imageRef.putFile(Uri.fromFile(tempFile), metadata).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            docRef.set(
                mapOf(
                    "tipo" to draft.tipo.label,
                    "nombre" to draft.nombre,
                    "serie" to draft.serie,
                    "imageUrl" to downloadUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            FigureAddResult.Success(id)
        } catch (t: Throwable) {
            FigureAddResult.Failure(t)
        } finally {
            tempFile?.delete()
        }
    }

    override suspend fun remove(id: String): FigureRemoveResult = try {
        // Delete the Firestore doc first so the user's list updates immediately via the
        // snapshot listener. The Storage blob is best-effort: a missing or already-deleted
        // image must not fail the whole operation (the figure the user cares about is gone).
        firestore.collection(COLLECTION).document(id).delete().await()
        runCatching { storage.reference.child("$COLLECTION/$id.jpg").delete().await() }
        FigureRemoveResult.Success
    } catch (t: Throwable) {
        FigureRemoveResult.Failure(t)
    }

    private suspend fun resizeToJpegFile(source: Uri): File = withContext(Dispatchers.IO) {
        // Two-pass decode: first read bounds only (cheap) to pick inSampleSize so the
        // raw bitmap never exceeds ~2× MAX_DIM in memory. Then decode scaled, apply EXIF
        // orientation, finally createScaledBitmap for the exact target dimension.
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        } ?: error("Could not open source image at $source")

        var sampleSize = 1
        val longestSrcEdge = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        while (longestSrcEdge / sampleSize > MAX_DIM * 2) sampleSize *= 2

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: error("Could not decode source image at $source")

        val oriented = applyExifOrientation(sampled, source)
        val scaled = fitToMaxDim(oriented, MAX_DIM)

        val outFile = File.createTempFile("figure_upload_", ".jpg", context.cacheDir)
        FileOutputStream(outFile).use { out ->
            // compress() returns false on native encoder failure without throwing;
            // without this check an empty/truncated temp file would upload as a "successful" add.
            check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                "JPEG compression failed for $source"
            }
        }

        if (scaled !== oriented) scaled.recycle()
        if (oriented !== sampled) oriented.recycle()
        sampled.recycle()

        outFile
    }

    private fun applyExifOrientation(bitmap: Bitmap, source: Uri): Bitmap {
        val orientation = context.contentResolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun fitToMaxDim(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (longestEdge <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longestEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun DocumentSnapshot.toFigure(): Figure? {
        val tipo = FigureType.fromLabel(getString("tipo")) ?: return null
        val nombre = getString("nombre") ?: return null
        return Figure(
            id = id,
            tipo = tipo,
            nombre = nombre,
            serie = getString("serie").orEmpty(),
            imageUrl = getString("imageUrl").orEmpty(),
        )
    }

    private companion object {
        const val COLLECTION = "figures"
        const val MAX_DIM = 1024
        const val JPEG_QUALITY = 80
        const val CACHE_CONTROL = "public, max-age=31536000, immutable"
    }
}
