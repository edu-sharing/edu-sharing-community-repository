package org.edu_sharing.repository.server.tools.security;


import org.apache.commons.codec.digest.HmacUtils;
import org.apache.log4j.Logger;



public class HMac {

    public static final String ALG_SHA256 = "HmacSHA256";


    Logger logger = Logger.getLogger(HMac.class);

    String sharedSecret;
    String algorithm;


    public HMac(String sharedSecret, String algorithm){
        this.sharedSecret = sharedSecret;
        this.algorithm = algorithm;
    }

    public String calculateHmac(String data) {
        //had problems using HmacUtils as class member and reuse, sometimes got another hex for the same data
        // so reinit
        HmacUtils hmacUtils = new HmacUtils(algorithm,sharedSecret);
        return hmacUtils.hmacHex(data);
    }

    public boolean checkHmac(String data, String hmacHex) {
        return calculateHmac(data).equals(hmacHex);
    }


    public static void main(String[] args) {
        HMac hmac = new HMac("b65823166d642a6a1b4bd5f26f075bb8", ALG_SHA256);
        long start = System.currentTimeMillis();
        for(int i = 0; i < 1; i++) {
            String rs = hmac.calculateHmac("thetest");
           /* if(!"e20325d776dc1c884b2ec69e6de6c79222b9e3546fd0b668d04fa5b9ccb2ddb9".equals(rs)){
                System.out.println("diff:" + rs);
            }*/
        }



        System.out.println("ready in "+(System.currentTimeMillis() -start)+"ms");
    }
}
