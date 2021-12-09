package com.bistu.ct.musicplayer;

import java.io.Serializable;
import java.util.List;


public class json implements Serializable {

    /**
     * data : [{"id":25638767,"url":"http://m10.music.126.net/20211202223110/ee6182d71e1b8f2870bc5a1c2700733c/ymusic/obj/w5zDlMODwrDDiGjCn8Ky/3047696181/760a/03ca/b381/f1ef28fd5f119116f4a8fa2b019af43c.flac","br":779506,"size":31232236,"md5":"f1ef28fd5f119116f4a8fa2b019af43c","code":200,"expi":1200,"type":"flac","gain":0,"fee":0,"uf":null,"payed":0,"flag":257,"canExtend":false,"freeTrialInfo":null,"level":null,"encodeType":null,"freeTrialPrivilege":{"resConsumable":false,"userConsumable":false},"freeTimeTrialPrivilege":{"resConsumable":false,"userConsumable":false,"type":0,"remainTime":0},"urlSource":0}]
     * code : 200
     */

    private int code;
    private List<DataBean> data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean implements Serializable {
        /**
         * id : 25638767
         * url : http://m10.music.126.net/20211202223110/ee6182d71e1b8f2870bc5a1c2700733c/ymusic/obj/w5zDlMODwrDDiGjCn8Ky/3047696181/760a/03ca/b381/f1ef28fd5f119116f4a8fa2b019af43c.flac
         * br : 779506
         * size : 31232236
         * md5 : f1ef28fd5f119116f4a8fa2b019af43c
         * code : 200
         * expi : 1200
         * type : flac
         * gain : 0.0
         * fee : 0
         * uf : null
         * payed : 0
         * flag : 257
         * canExtend : false
         * freeTrialInfo : null
         * level : null
         * encodeType : null
         * freeTrialPrivilege : {"resConsumable":false,"userConsumable":false}
         * freeTimeTrialPrivilege : {"resConsumable":false,"userConsumable":false,"type":0,"remainTime":0}
         * urlSource : 0
         */

        private int id;
        private String url;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }}