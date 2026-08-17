package com.malik.indiancourtdiary
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class HearingResponse(val date:String?,val purpose:String?,val judge:String?,val status:String?)
data class CaseResponse(
 val cnr:String,
 val caseTitle:String,
 val courtName:String,
 val nextHearingDate:String?,
 val stage:String,
 val hearingHistory:List<HearingResponse> = emptyList()
)
interface CourtApi{@GET("v1/cases/{cnr}")suspend fun getCase(@Path("cnr")cnr:String):CaseResponse}
object CourtApiProvider{val api:CourtApi by lazy{
 val client=OkHttpClient.Builder().addInterceptor{chain->
  val builder=chain.request().newBuilder()
  if(BuildConfig.APP_CLIENT_KEY.isNotBlank())builder.header("X-App-Key",BuildConfig.APP_CLIENT_KEY)
  chain.proceed(builder.build())
 }.build()
 Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(CourtApi::class.java)}}
