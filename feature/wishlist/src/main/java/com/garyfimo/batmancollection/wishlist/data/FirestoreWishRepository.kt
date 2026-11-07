package com.garyfimo.batmancollection.wishlist.data

import com.garyfimo.batmancollection.core.domain.FigureType
import com.garyfimo.batmancollection.wishlist.domain.WishAddResult
import com.garyfimo.batmancollection.wishlist.domain.WishDraft
import com.garyfimo.batmancollection.wishlist.domain.WishItem
import com.garyfimo.batmancollection.wishlist.domain.WishListResult
import com.garyfimo.batmancollection.wishlist.domain.WishRemoveResult
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreWishRepository(
    private val firestore: FirebaseFirestore,
) : WishRepository {

    override fun observe(): Flow<WishListResult> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(WishListResult.Failure(error))
                    snapshot != null -> trySend(
                        WishListResult.Success(snapshot.documents.mapNotNull { it.toWishItem() }),
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun add(draft: WishDraft): WishAddResult = try {
        val docRef = firestore.collection(COLLECTION).document()
        docRef.set(
            mapOf(
                "tipo" to draft.tipo.label,
                "nombre" to draft.nombre,
                "serie" to draft.serie,
                "url" to draft.url,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        WishAddResult.Success(docRef.id)
    } catch (t: Throwable) {
        WishAddResult.Failure(t)
    }

    override suspend fun remove(id: String): WishRemoveResult = try {
        firestore.collection(COLLECTION).document(id).delete().await()
        WishRemoveResult.Success
    } catch (t: Throwable) {
        WishRemoveResult.Failure(t)
    }

    private fun DocumentSnapshot.toWishItem(): WishItem? {
        val tipo = FigureType.fromLabel(getString("tipo")) ?: return null
        val nombre = getString("nombre") ?: return null
        return WishItem(
            id = id,
            tipo = tipo,
            nombre = nombre,
            serie = getString("serie").orEmpty(),
            url = getString("url").orEmpty(),
        )
    }

    private companion object {
        const val COLLECTION = "wishlist"
    }
}
