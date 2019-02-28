/*******************************************************************************
* Copyright (c) 2017 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.acmeair.securityutils;



import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SecurityUtils { 
      
  // TODO: Hardcode for now
  private static final String secretKey = "acmeairsecret128";
    
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String SHA_256 = "SHA-256";
  private static final String UTF8 = "UTF-8";

  private static Mac macCached = null;
  
  /* microprofile-1.1 */
  @Inject 
  @ConfigProperty(name = "SECURE_USER_CALLS", defaultValue = "true") 
  private Boolean secureUserCalls; 

  /* microprofile-1.1 */
  @Inject @ConfigProperty(name = "SECURE_SERVICE_CALLS", defaultValue = "true")
  private Boolean secureServiceCalls;
  
  @PostConstruct
  private void initialize() {
    
    System.out.println("SECURE_USER_CALLS: " + secureUserCalls);
    System.out.println("SECURE_SERVICE_CALLS: " + secureServiceCalls);
  }
    
  private static final ThreadLocal<MessageDigest> localSHA256 =
      new ThreadLocal<MessageDigest>() {
    @Override protected MessageDigest initialValue() {
      try {
        return MessageDigest.getInstance(SHA_256);
      } catch (NoSuchAlgorithmException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
      }
    }
  };
    
  private static final ThreadLocal<Mac> localMac =
      new ThreadLocal<Mac>() {
    @Override protected Mac initialValue() {
      Mac toReturn = null;
      try {
        toReturn = Mac.getInstance(HMAC_ALGORITHM);
        toReturn.init(new SecretKeySpec(secretKey.getBytes(UTF8), HMAC_ALGORITHM));
      } catch (Exception e) {
        e.printStackTrace();
      }
      return toReturn;
    }
    };
  
  
  public boolean secureUserCalls() {
    return secureUserCalls;
  }
  
  public boolean secureServiceCalls() {
    return secureServiceCalls;
  }
    
  /**
   * Build Hash of data.
   */
  public String buildHash(String data)
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest md = localSHA256.get();
    md.update(data.getBytes(UTF8));
    byte[] digest = md.digest();
    return Base64.getEncoder().encodeToString(digest);
  }
   
  /**
   * Build signature of all this junk.
   */
  public String buildHmac(String method, String baseUri, String userId, String dateString, 
      String sigBody)
          throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {

    List<String> toHash = new ArrayList<String>();
    toHash.add(method);
    toHash.add(baseUri);
    toHash.add(userId);
    toHash.add(dateString);
    toHash.add(sigBody);   

    Mac mac = localMac.get();
    for (String s: toHash) {
      mac.update(s.getBytes(UTF8));
    }

    return Base64.getEncoder().encodeToString(mac.doFinal());
  }
  
  
  /**
   * Verify the bodyHash.
   */
  public boolean verifyBodyHash(String body, String sigBody) {
        
    if (sigBody.isEmpty()) {
      throw new WebApplicationException("Invalid signature (sigBody)", Status.FORBIDDEN);
    }

    if (body == null || body.length() == 0) {
      throw new WebApplicationException("Invalid signature (body)", Status.FORBIDDEN);
    }

    try {
      String bodyHash = buildHash(body);
      if (!sigBody.equals(bodyHash)) {
        throw new WebApplicationException("Invalid signature (bodyHash)", Status.FORBIDDEN);
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      throw new WebApplicationException("Unable to generate hash", Status.FORBIDDEN);
    }

    return true;
  }
  
  /**
   * Verify the signature junk.
   */
  public boolean verifyFullSignature(String method, 
          String baseUri, 
          String userId, 
          String dateString, 
          String sigBody,
          String signature) {
    try {
      String hmac = buildHmac(method,baseUri,userId,dateString,sigBody);
            
      if (!signature.equals(hmac)) {
        throw new WebApplicationException("Invalid signature (hmacCompare)", 
                  Status.FORBIDDEN);
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
      throw new WebApplicationException("Invalid signature", Status.FORBIDDEN);
    }

    return true;
  }
  
}
