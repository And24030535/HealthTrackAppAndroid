package com.itc.healthtrackandroid.dao

// callback para acciones que no devuelven datos como guardar o borrar
interface OnOperationCompleteListener {
    fun onSuccess()
    fun onFailure(error: Exception)
}

// callback para pedir una lista completa de datos donde T es el tipo (User o Metric)
interface OnDataLoadedListener<T> {
    fun onSuccess(data: List<T>)
    fun onFailure(error: Exception)
}

// callback para buscar un solo documento por su id
interface OnSingleDataLoadedListener<T> {
    // devuelve el objeto o nulo si no se encontro
    fun onSuccess(data: T?)
    fun onFailure(error: Exception)
}