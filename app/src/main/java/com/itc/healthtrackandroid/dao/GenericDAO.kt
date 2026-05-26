package com.itc.healthtrackandroid.dao

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// clase generica para manejar la base de datos que sirve para cualquier modelo sin repetir codigo
class GenericDAO<T : Any>(
    private val entityClass: Class<T>,
    private val collectionName: String
) {
    private val db = FirebaseFirestore.getInstance()

    // genera un id aleatorio directo desde Firebase para que no se repitan los identificadores
    fun createDocumentId(): String {
        return db.collection(collectionName).document().id
    }

    // guarda o actualiza un documento en la base de datos
    fun save(documentId: String, entity: T, callback: OnOperationCompleteListener) {
        db.collection(collectionName).document(documentId).set(entity)
            .addOnSuccessListener { callback.onSuccess() }
            .addOnFailureListener { exception -> callback.onFailure(exception) }
    }

    // busca un solo elemento usando su id especifico
    fun getById(documentId: String, callback: OnSingleDataLoadedListener<T>) {
        db.collection(collectionName).document(documentId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // convertimos el documento de internet en un objeto de Kotlin
                    val entity = document.toObject(entityClass)
                    callback.onSuccess(entity)
                } else {
                    // si no existe devolvemos nulo
                    callback.onSuccess(null)
                }
            }
            .addOnFailureListener { exception -> callback.onFailure(exception) }
    }

    // registra un listener en tiempo real para documentos donde campo es igual a valor (cancelar con remove en onDestroy)
    fun listenByField(
        fieldName: String,
        value: Any,
        callback: OnDataLoadedListener<T>
    ): ListenerRegistration {
        return db.collection(collectionName)
            .whereEqualTo(fieldName, value)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    callback.onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                // convertimos cada documento de firestore en un objeto kotlin y lo agregamos a la lista
                val list = mutableListOf<T>()
                for (doc in snapshots) {
                    val entity = doc.toObject(entityClass)
                    list.add(entity)
                }
                callback.onSuccess(list)
            }
    }
}