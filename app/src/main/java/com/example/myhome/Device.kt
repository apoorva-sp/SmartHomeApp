package com.example.myhome

data class Device(
    val name: String,
    val type: String,
    val icon: String,
    var isOn: Boolean
)