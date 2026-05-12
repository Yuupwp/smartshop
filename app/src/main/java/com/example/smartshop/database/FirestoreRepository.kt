package com.example.smartshop.repository

import com.example.smartshop.model.Abarrote
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

    // Nombre de la colección en Firestore
    private val coleccion = db.collection("abarrotes")

    // ─────────────────────────────────────────────────
    // AGREGAR un abarrote nuevo
    // ─────────────────────────────────────────────────
    suspend fun agregarAbarrote(abarrote: Abarrote): Result<String> {
        return try {
            val docRef = coleccion.add(abarrote).await()
            // Actualizar el campo "id" con el ID generado por Firestore
            coleccion.document(docRef.id)
                .update("id", docRef.id)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────
    // LEER todos los abarrotes (una sola vez)
    // ─────────────────────────────────────────────────
    suspend fun obtenerAbarrotes(): Result<List<Abarrote>> {
        return try {
            val snapshot = coleccion.get().await()
            val lista = snapshot.documents.mapNotNull { it.toObject<Abarrote>() }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────
    // ESCUCHAR cambios en tiempo real (Flow)
    // Usa esto para actualizar la UI automáticamente
    // ─────────────────────────────────────────────────
    fun escucharAbarrotes(): Flow<List<Abarrote>> = callbackFlow {
        val listener = coleccion.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents?.mapNotNull { it.toObject<Abarrote>() } ?: emptyList()
            trySend(lista)
        }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────
    // ACTUALIZAR un abarrote existente por ID
    // ─────────────────────────────────────────────────
    suspend fun actualizarAbarrote(id: String, abarrote: Abarrote): Result<Unit> {
        return try {
            coleccion.document(id)
                .set(abarrote.copy(id = id, fechaActualizacion = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────
    // ELIMINAR un abarrote por ID
    // ─────────────────────────────────────────────────
    suspend fun eliminarAbarrote(id: String): Result<Unit> {
        return try {
            coleccion.document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────
    // BUSCAR por nombre
    // ─────────────────────────────────────────────────
    suspend fun buscarPorNombre(nombre: String): Result<List<Abarrote>> {
        return try {
            val snapshot = coleccion
                .whereGreaterThanOrEqualTo("nombre", nombre)
                .whereLessThanOrEqualTo("nombre", nombre + "\uF8FF")
                .get()
                .await()
            val lista = snapshot.documents.mapNotNull { it.toObject<Abarrote>() }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
