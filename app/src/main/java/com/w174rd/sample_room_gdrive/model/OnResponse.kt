package com.w174rd.sample_room_gdrive.model

class OnResponse<T> private constructor(var status: Int, var data: T? = null, var error: T? = null) {

    companion object {

        const val LOADING = 0
        const val SUCCESS = 1
        const val ERROR = 2

        /** Creates a new loading resource object  */
        fun <T> loading(data: T? = null): OnResponse<T> {
            return OnResponse(status = LOADING,
                data = data)
        }

        /**
         * Creates a new successful resource object.
         * @param data the data to be set
         */
        fun <T> success(data: T? = null): OnResponse<T> {
            return OnResponse(status = SUCCESS,
                data = data)
        }

        /**
         * Creates a new error resource object.
         * @param error the error
         */
        fun <T> error(error: T, data: T? = null): OnResponse<T> {
            return OnResponse(status = ERROR,
                data = data,
                error = error)
        }
    }
}