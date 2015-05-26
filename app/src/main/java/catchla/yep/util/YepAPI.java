package catchla.yep.util;

import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.annotation.method.PATCH;
import org.mariotaku.restfu.annotation.method.POST;
import org.mariotaku.restfu.annotation.method.PUT;
import org.mariotaku.restfu.annotation.param.Body;
import org.mariotaku.restfu.annotation.param.Form;
import org.mariotaku.restfu.http.BodyType;

import catchla.yep.model.AccessToken;
import catchla.yep.model.Client;
import catchla.yep.model.CreateRegistrationResult;
import catchla.yep.model.ProfileUpdate;
import catchla.yep.model.User;
import catchla.yep.model.VerificationMethod;

/**
 * Created by mariotaku on 15/5/12.
 */
public interface YepAPI {

    @POST("/v1/registration/create")
    @Body(BodyType.FORM)
    CreateRegistrationResult createRegistration(@Form("mobile") String mobile,
                                                @Form("phone_code") String phoneCode,
                                                @Form("nickname") String nickname,
                                                @Form("longitude") double longitude,
                                                @Form("latitude") double latitude) throws YepException;

    @PUT("/v1/registration/update")
    @Body(BodyType.FORM)
    AccessToken updateRegistration(@Form("mobile") String mobile,
                                   @Form("phone_code") String phoneCode,
                                   @Form("token") String token,
                                   @Form("client") Client client,
                                   @Form("expiring") long expiringInseconds) throws YepException;

    @PUT("/auth/token_by_mobile")
    @Body(BodyType.FORM)
    AccessToken tokenByMobile(@Form("mobile") String mobile,
                              @Form("phone_code") String phoneCode,
                              @Form("token") String token,
                              @Form("client") Client client,
                              @Form("expiring") long expiringInseconds) throws YepException;

    @POST("/v1/sms_verification_codes")
    @Body(BodyType.FORM)
    void sendVerifyCode(@Form("mobile") String mobile,
                        @Form("phone_code") String phoneCode,
                        @Form("method") VerificationMethod method) throws YepException;

    @PATCH("/v1/user")
    User updateProfile(@Form ProfileUpdate profileUpdate) throws YepException;

    @GET("/v1/user")
    User getUser() throws YepException;
}

