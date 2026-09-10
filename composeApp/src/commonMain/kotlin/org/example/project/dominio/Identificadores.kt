package org.example.project.dominio

import java.util.UUID

fun nuevoId(): String = UUID.randomUUID().toString()
