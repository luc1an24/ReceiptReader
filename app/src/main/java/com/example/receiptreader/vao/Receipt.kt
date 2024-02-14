package com.example.receiptreader.vao

data class Receipt(
    var picture: ByteArray,
    var shop: String,
    var cost: Double,
    var lat: Double,
    var lon: Double,
    var timestamp: Long,
    var perfectlyRead: Boolean
)