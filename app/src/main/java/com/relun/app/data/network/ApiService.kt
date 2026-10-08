package com.relun.app.data.network

import com.relun.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {

    // ---------- Auth ----------
    @POST("api/auth/request-otp")
    suspend fun requestOtp(@Body body: ContactBody): RequestOtpResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpBody): VerifyOtpResponse

    @POST("api/auth/logout")
    suspend fun logout(): ResponseBody

    @DELETE("api/auth/account")
    suspend fun deleteAccount(): ResponseBody

    // ---------- My profile ----------
    @GET("api/profiles")
    suspend fun myProfile(): MyProfileResponse

    @POST("api/profiles/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileBody): ResponseBody

    @PUT("api/profiles")
    suspend fun updateProfile(@Body body: UpdateProfileBody): ProfileDto

    @Multipart
    @POST("api/profiles/photos")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): PhotoDto

    @Multipart
    @PATCH("api/profiles/photos/{photoId}")
    suspend fun replacePhoto(@Path("photoId") photoId: String, @Part photo: MultipartBody.Part): PhotoDto

    @DELETE("api/profiles/photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: String): ResponseBody

    // ---------- People ----------
    @GET("api/profiles/discover")
    suspend fun discover(
        @Query("latitude") latitude: Double?,
        @Query("longitude") longitude: Double?,
        @Query("limit") limit: Int = 50,
    ): DiscoverResponse

    @GET("api/profiles/{userId}")
    suspend fun person(@Path("userId") userId: String): PersonCardDto

    @POST("api/profiles/likes/{userId}")
    suspend fun like(@Path("userId") userId: String): LikeResponse

    @DELETE("api/profiles/likes/{userId}")
    suspend fun unlike(@Path("userId") userId: String): ResponseBody

    @POST("api/profiles/passes/{userId}")
    suspend fun pass(@Path("userId") userId: String): ResponseBody

    @GET("api/profiles/matches")
    suspend fun matches(): MatchesResponse

    @GET("api/profiles/likes/received")
    suspend fun receivedLikes(): LockedPeopleResponse

    @GET("api/profiles/views")
    suspend fun profileViews(): LockedPeopleResponse

    // ---------- Chat ----------
    @GET("api/chat")
    suspend fun conversations(): ConversationsResponse

    @GET("api/chat/unread/count")
    suspend fun unreadCount(): UnreadCountResponse

    @GET("api/chat/{userId}")
    suspend fun messages(
        @Path("userId") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): MessagesResponse

    @POST("api/chat/{userId}/request")
    suspend fun sendMessageRequest(@Path("userId") userId: String, @Body body: SendRequestBody): SendRequestResponse

    @POST("api/chat/{userId}/request/decline")
    suspend fun declineMessageRequest(@Path("userId") userId: String): ResponseBody

    // ---------- Coins ----------
    @GET("api/coins")
    suspend fun wallet(): WalletResponse

    @POST("api/coins/purchases")
    suspend fun confirmPurchase(@Body body: PurchaseBody): PurchaseResponse


    @POST("api/coins/bonus/{id}/seen")
    suspend fun markBonusSeen(@Path("id") id: String): ResponseBody

    @POST("api/coins/insights")
    suspend fun buyInsights(@Body body: InsightsBody): InsightsResponse

    // ---------- Dates ----------
    @GET("api/dates")
    suspend fun browseDates(): DatesResponse

    @GET("api/dates/mine")
    suspend fun myDates(): DatesResponse

    @POST("api/dates")
    suspend fun createDate(@Body body: CreateDateBody): CreateDateResponse

    @DELETE("api/dates/{dateId}")
    suspend fun deleteDate(@Path("dateId") dateId: String): ResponseBody

    @POST("api/dates/{dateId}/requests")
    suspend fun requestToJoin(@Path("dateId") dateId: String): JoinResponse

    @PATCH("api/dates/{dateId}/requests/{requestId}")
    suspend fun respondToRequest(
        @Path("dateId") dateId: String,
        @Path("requestId") requestId: String,
        @Body body: RequestStatusBody,
    ): ResponseBody

    // ---------- Safety ----------
    @POST("api/safety/blocks/{userId}")
    suspend fun block(@Path("userId") userId: String): ResponseBody

    @DELETE("api/safety/blocks/{userId}")
    suspend fun unblock(@Path("userId") userId: String): ResponseBody

    @GET("api/safety/blocks")
    suspend fun blocks(): BlocksResponse

    @POST("api/safety/reports/{userId}")
    suspend fun report(@Path("userId") userId: String, @Body body: ReportBody): ResponseBody

    // ---------- Relun Plus ----------
    @GET("api/plus")
    suspend fun plus(): EntitlementsResponse

    /** After Google Play reports the subscription, before acknowledging it. */
    @POST("api/plus/play")
    suspend fun confirmPlayPlus(@Body body: PlayPlusBody): EntitlementsResponse

    // ---------- Settings & push ----------
    @GET("api/settings")
    suspend fun settings(): SettingsResponse

    @PATCH("api/settings")
    suspend fun updateSettings(@Body body: SettingsPatch): SettingsResponse

    @POST("api/notifications/token")
    suspend fun registerPushToken(@Body body: PushTokenBody): ResponseBody

    @HTTP(method = "DELETE", path = "api/notifications/token", hasBody = true)
    suspend fun deletePushToken(@Body body: PushTokenBody): ResponseBody
}
