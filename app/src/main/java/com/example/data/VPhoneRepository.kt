package com.example.data

import kotlinx.coroutines.flow.Flow

class VPhoneRepository(private val vPhoneDao: VPhoneDao) {
    val allPhones: Flow<List<VPhone>> = vPhoneDao.getAllPhones()

    fun getPhoneById(id: Int): Flow<VPhone?> = vPhoneDao.getPhoneById(id)

    suspend fun insert(phone: VPhone): Long = vPhoneDao.insertPhone(phone)

    suspend fun update(phone: VPhone) = vPhoneDao.updatePhone(phone)

    suspend fun delete(phone: VPhone) = vPhoneDao.deletePhone(phone)

    suspend fun deleteById(id: Int) = vPhoneDao.deletePhoneById(id)
}
