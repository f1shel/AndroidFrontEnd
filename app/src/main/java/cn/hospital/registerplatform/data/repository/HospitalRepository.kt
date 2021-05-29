package cn.hospital.registerplatform.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import cn.hospital.registerplatform.api.Resource
import cn.hospital.registerplatform.api.interfaces.HospitalApi
import cn.hospital.registerplatform.data.UserPreference
import cn.hospital.registerplatform.data.dto.CountType
import cn.hospital.registerplatform.data.dto.RawResult
import cn.hospital.registerplatform.data.pagingsource.HospitalPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class HospitalRepository(
    private val hospitalApi: HospitalApi,
    private val userPreference: UserPreference,
    private val pagingConfig: PagingConfig
) {
    fun getHospitalList() = getList { countType, page, size ->
        hospitalApi.getHospitalList(countType, page, size)
    }

    fun getDepartmentList(hospitalId: Int) = getList { countType, page, size ->
        hospitalApi.getDepartmentList(hospitalId, countType, page, size)
    }

    fun getDoctorList(departmentId: Int) = getList { countType, page, size ->
        hospitalApi.getDoctorList(departmentId, countType, page, size)
    }

    private fun <T : Any> getList(
        getListFromApi: suspend (CountType, page: Int, size: Int) -> RawResult<List<T>>
    ): Flow<PagingData<T>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                HospitalPagingSource { page: Int, size: Int ->
                    try {
                        val rawResult = getListFromApi(CountType.PAGE, page, size)
                        if (rawResult.success) rawResult.content else listOf()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        listOf()
                    }
                }
            }
        ).flow.flowOn(Dispatchers.IO)
    }

    fun getHospitalInfo(hospitalId: Int) = getInfo {
        hospitalApi.getHospitalInfo(hospitalId)
    }

    fun getDepartmentInfo(departmentId: Int) = getInfo {
        hospitalApi.getDepartmentInfo(departmentId)
    }

    fun getDoctorInfo(doctorId: Int) = getInfo {
        hospitalApi.getDoctorInfo(doctorId)
    }

    private fun <T> getInfo(
        getInfoFromApi: suspend () -> RawResult<T>
    ): Flow<Resource<T>> = flow {
        try {
            val rawResult = getInfoFromApi()
            if (rawResult.success) {
                emit(Resource.Success(rawResult.content))
            } else {
                emit(Resource.Failure(null))
            }
        } catch (e: Exception) {
            emit(Resource.Failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
