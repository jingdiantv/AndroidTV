package com.kt.apps.autoupdate

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun Context.isSelfUpdateDelegate(): Boolean {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        applicationContext.packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
    } else {
        applicationContext.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    }
    val permissions = info.requestedPermissions
    return permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")
}

fun File.calculateMD5(): String? {
    val digest: MessageDigest
    val inputStream: InputStream
    try {
        digest = MessageDigest.getInstance("MD5")
        inputStream = FileInputStream(this)
    } catch (e: NoSuchAlgorithmException) {
        return null
    } catch (e: FileNotFoundException) {
        return null
    }
    val buffer = ByteArray(8192)
    var read: Int
    return try {
        while (inputStream.read(buffer).also { read = it } > 0) {
            digest.update(buffer, 0, read)
        }
        val md5sum = digest.digest()
        val bigInt = BigInteger(1, md5sum)
        var output = bigInt.toString(16)
        output = String.format("%32s", output).replace(' ', '0')
        output
    } catch (e: IOException) {
        null
    } finally {
        try {
            inputStream.close()
        } catch (ignored: IOException) {
        }
    }
}

fun File.checkSum(sum: String): Boolean {
    val md5 = this.calculateMD5() ?: return false
    return md5.equals(sum, ignoreCase = false)
}