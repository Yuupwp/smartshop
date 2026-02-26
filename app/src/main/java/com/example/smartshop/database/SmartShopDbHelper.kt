package com.example.smartshop.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SmartShopDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE productos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                precio REAL NOT NULL,
                stock INTEGER NOT NULL DEFAULT 0,
                stock_minimo INTEGER NOT NULL DEFAULT 5
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE ventas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha_ms INTEGER NOT NULL,
                total REAL NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE venta_detalle(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                venta_id INTEGER NOT NULL,
                producto_id INTEGER NOT NULL,
                cantidad INTEGER NOT NULL,
                precio_unitario REAL NOT NULL,
                subtotal REAL NOT NULL,
                FOREIGN KEY(venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
                FOREIGN KEY(producto_id) REFERENCES productos(id) ON DELETE RESTRICT
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS venta_detalle")
        db.execSQL("DROP TABLE IF EXISTS ventas")
        db.execSQL("DROP TABLE IF EXISTS productos")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "smartshop.db"
        private const val DATABASE_VERSION = 1
    }
}