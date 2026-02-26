package com.example.smartshop.database

import android.content.ContentValues
import android.content.Context

class SmartShopRepository(context: Context) {

    private val dbHelper = SmartShopDbHelper(context.applicationContext)

    fun insertarProducto(nombre: String, precio: Double, stock: Int, stockMinimo: Int = 5): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("precio", precio)
            put("stock", stock)
            put("stock_minimo", stockMinimo)
        }
        return db.insertOrThrow("productos", null, values)
    }

    fun contarVentasDelDia(inicioMs: Long, finMs: Long): Int {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT COUNT(*) FROM ventas WHERE fecha_ms >= ? AND fecha_ms < ?",
            arrayOf(inicioMs.toString(), finMs.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun totalVentasDelDia(inicioMs: Long, finMs: Long): Double {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT COALESCE(SUM(total),0) FROM ventas WHERE fecha_ms >= ? AND fecha_ms < ?",
            arrayOf(inicioMs.toString(), finMs.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }
}