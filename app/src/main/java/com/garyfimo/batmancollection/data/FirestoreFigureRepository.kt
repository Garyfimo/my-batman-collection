package com.garyfimo.batmancollection.data

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirestoreFigureRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : FigureRepository {

    override suspend fun list(): FigureListResult = try {
        val snapshot = firestore.collection(COLLECTION).get().await()
        FigureListResult.Success(snapshot.documents.mapNotNull { it.toFigure() })
    } catch (t: Throwable) {
        FigureListResult.Failure(t)
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
