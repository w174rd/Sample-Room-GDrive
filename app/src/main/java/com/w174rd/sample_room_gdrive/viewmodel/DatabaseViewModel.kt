package com.w174rd.sample_room_gdrive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w174rd.sample_room_gdrive.db.DataBase
import com.w174rd.sample_room_gdrive.model.Entity
import com.w174rd.sample_room_gdrive.model.OnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DatabaseViewModel: ViewModel() {

    val status = MutableLiveData<OnResponse<Any>>()
    val responseData = MutableLiveData<ArrayList<Entity>>()

    fun getData(db: DataBase) {
        viewModelScope.launch(Dispatchers.Default) {
            val data = db.entityDao().getAll()
            val dataList = ArrayList<Entity>()
            dataList.addAll(data)
            responseData.postValue(dataList)
        }
    }

    fun insertData(db: DataBase, data: Entity?) {
        status.postValue(OnResponse.loading())
        viewModelScope.launch(Dispatchers.Default) {
            data?.let {
                db.entityDao().insert(data = it)
                getData(db = db)
                status.postValue(OnResponse.success())
            }
        }
    }

}