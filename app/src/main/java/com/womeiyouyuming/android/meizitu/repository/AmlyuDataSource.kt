package com.womeiyouyuming.android.meizitu.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.womeiyouyuming.android.meizitu.model.Photo
import com.womeiyouyuming.android.meizitu.network.NetworkStatus
import com.womeiyouyuming.android.meizitu.util.parseAmlyu
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by Yue on 2019/12/19.
 */

//Amlyu的paging数据源，loadBefore用不到，没有写
class AmlyuDataSource(private val photoRepository: PhotoRepository) :
    PageKeyedDataSource<Int, Photo>() {

    //加载状态
    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus

    private var retry: (() -> Unit)? = null


    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Photo>
    ) {

        _networkStatus.postValue(NetworkStatus.FIRST_LOADING)

        retry = null
        photoRepository.getPhotosFromAmlyuPaging(1).enqueue(object : Callback<ResponseBody> {

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                _networkStatus.postValue(NetworkStatus.FAILED)
                retry = {
                    loadInitial(params, callback)
                }
                t.printStackTrace()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val result = parseAmlyu(response.body()?.string())
                _networkStatus.postValue(NetworkStatus.SUCCESS)
                callback.onResult(result, null, 2)
            }
        })
    }


    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Photo>) {

        _networkStatus.postValue(NetworkStatus.LOADING)


        retry = null
        photoRepository.getPhotosFromAmlyuPaging(params.key)
            .enqueue(object : Callback<ResponseBody> {

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    _networkStatus.postValue(NetworkStatus.FAILED)
                    retry = {
                        loadAfter(params, callback)
                    }
                    t.printStackTrace()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    val result = parseAmlyu(response.body()?.string())
                    _networkStatus.postValue(NetworkStatus.SUCCESS)
                    callback.onResult(result, params.key + 1)
                }
            })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Photo>) {

    }

    fun retryFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            it()
        }
    }


}