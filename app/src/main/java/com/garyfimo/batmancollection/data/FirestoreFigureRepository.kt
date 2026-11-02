package com.garyfimo.batmancollection.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreFigureRepository(
    private val firestore: FirebaseFirestore,
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
