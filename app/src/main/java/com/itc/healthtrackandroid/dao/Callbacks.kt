package com.itc.healthtrackandroid.dao

// usamos este callback cuando hacemos una operacion que no devuelve datos como guardar o borrar
interface OnOperationCompleteListener {
    fun onSuccess()
    fun onFailure(error: Exception)
}

// usamos este callback cuando pedimos una lista completa de datos y T puede ser cualquier modelo
interface OnDataLoadedListener<T> {
    fun onSuccess(data: List<T>)
    fun onFailure(error: Exception)
}

// usamos este callback cuando buscamos un solo documento por su id
interface OnSingleDataLoadedListener<T> {
    // devuelve el objeto encontrado o null si no existia en Firestore
    fun onSuccess(data: T?)
    fun onFailure(error: Exception)
}
