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

    fun obtenerProductos(): List<Map<String, Any>> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Map<String, Any>>()
        db.rawQuery("SELECT * FROM productos", null).use { c ->
            while (c.moveToNext()) {
                lista.add(mapOf(
                    "id" to c.getInt(c.getColumnIndexOrThrow("id")),
                    "nombre" to c.getString(c.getColumnIndexOrThrow("nombre")),
                    "precio" to c.getDouble(c.getColumnIndexOrThrow("precio")),
                    "stock" to c.getInt(c.getColumnIndexOrThrow("stock")),
                    "stock_minimo" to c.getInt(c.getColumnIndexOrThrow("stock_minimo"))
                ))
            }
        }
        return lista
    }

    fun actualizarProducto(id: Int, nombre: String, precio: Double, stock: Int, stockMinimo: Int = 5): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("precio", precio)
            put("stock", stock)
            put("stock_minimo", stockMinimo)
        }
        return db.update("productos", values, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarProducto(id: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete("productos", "id = ?", arrayOf(id.toString()))
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
