package com.garyfimo.batmancollection.figure.data

import android.net.Uri
import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureAddResult
import com.garyfimo.batmancollection.figure.domain.FigureDraft
import com.garyfimo.batmancollection.figure.domain.FigureListResult
import com.garyfimo.batmancollection.figure.domain.FigureLookup
import com.garyfimo.batmancollection.figure.domain.FigureRemoveResult
import com.garyfimo.batmancollection.figure.domain.FigureRepository
import com.garyfimo.batmancollection.figure.domain.FigureType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreFigureRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
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

    override suspend fun add(draft: FigureDraft): FigureAddResult = try {
        // Reserve a Firestore doc id up front so the Storage path and the Firestore
        // doc share it. Picking Firestore's auto-id instead of a slug-based one keeps
        // the client write path collision-free and side-steps a "does this slug already
        // exist?" round-trip.
        val docRef = firestore.collection(COLLECTION).document()
        val id = docRef.id

        val imageRef = storage.reference.child("$COLLECTION/$id.jpg")
        imageRef.putFile(Uri.parse(draft.imageUri)).await()
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
    }
}
