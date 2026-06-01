package com.itc.healthtrackandroid.dao

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// clase generica que centraliza todas las operaciones de Firestore para que no tengamos que
// repetir el mismo codigo de lectura y escritura en cada pantalla del proyecto
class GenericDAO<T : Any>(
    private val entityClass: Class<T>,
    private val collectionName: String
) {
    private val db = FirebaseFirestore.getInstance()

    // le pedimos a Firestore un id unico nuevo para que nunca se repita en la coleccion
    fun createDocumentId(): String {
        return db.collection(collectionName).document().id
    }

    // guarda o actualiza el documento en Firestore usando el id que se le pasa
    fun save(documentId: String, entity: T, callback: OnOperationCompleteListener) {
        db.collection(collectionName).document(documentId).set(entity)
            .addOnSuccessListener { callback.onSuccess() }
            .addOnFailureListener { exception -> callback.onFailure(exception) }
    }

    // busca un solo documento por su id y lo devuelve convertido en objeto o null si no existe
    fun getById(documentId: String, callback: OnSingleDataLoadedListener<T>) {
        db.collection(collectionName).document(documentId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val entity = document.toObject(entityClass)
                    callback.onSuccess(entity)
                } else {
                    // si el documento no existe le avisamos con null en lugar de lanzar error
                    callback.onSuccess(null)
                }
            }
            .addOnFailureListener { exception -> callback.onFailure(exception) }
    }

    // borra permanentemente un documento de Firestore usando su id
    fun delete(documentId: String, callback: OnOperationCompleteListener) {
        db.collection(collectionName).document(documentId).delete()
            .addOnSuccessListener { callback.onSuccess() }
            .addOnFailureListener { exception -> callback.onFailure(exception) }
    }

    // abre un listener en tiempo real sobre toda la coleccion y hay que cerrarlo con remove en onDestroy
    fun listenAll(callback: OnDataLoadedListener<T>): ListenerRegistration {
        return db.collection(collectionName)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    callback.onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshots == null) return@addSnapshotListener
                val list = mutableListOf<T>()
                for (doc in snapshots) {
                    val entity = doc.toObject(entityClass)
                    list.add(entity)
                }
                callback.onSuccess(list)
            }
    }

    // descarga todos los documentos una sola vez sin dejar ningun listener abierto
    fun getAll(callback: OnDataLoadedListener<T>) {
        db.collection(collectionName).get()
            .addOnSuccessListener { snapshots ->
                val list = snapshots.documents.mapNotNull { it.toObject(entityClass) }
                callback.onSuccess(list)
            }
            .addOnFailureListener { callback.onFailure(it) }
    }

    // descarga una sola vez los documentos donde el campo dado tiene exactamente el valor dado
    fun getByField(fieldName: String, value: Any, callback: OnDataLoadedListener<T>) {
        db.collection(collectionName).whereEqualTo(fieldName, value).get()
            .addOnSuccessListener { snapshots ->
                val list = snapshots.documents.mapNotNull { it.toObject(entityClass) }
                callback.onSuccess(list)
            }
            .addOnFailureListener { callback.onFailure(it) }
    }

    // abre un listener en tiempo real filtrando por campo igual a valor y hay que cerrarlo con remove en onDestroy
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
                // convertimos cada documento de Firestore en su objeto Kotlin correspondiente
                val list = mutableListOf<T>()
                for (doc in snapshots) {
                    val entity = doc.toObject(entityClass)
                    list.add(entity)
                }
                callback.onSuccess(list)
            }
    }
}
