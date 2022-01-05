package com.gnss.ntripserial.Utils;

import java.math.BigDecimal;

public class GGAParse {
    private String originGGA;

    private String Status = "";
    private String Latitude;//纬度
    private String Longitude;//经度
    private double H = -1;//大地高
    private int sat_num;//卫星数
    private String age = "";//差分时间
    private String UTC;//时间

    public GGAParse(String originGGA) {
        this.originGGA = originGGA;
    }

    public GGAParse parse(){
        String[] fields = originGGA.split(",");
        if (fields.length<15 || !fields[0].contains("GGA"))
            throw new RuntimeException("illegal gga format");

        if (!" ".equals(fields[1])) {
            String time = fields[1];
            String hour = time.substring(0, 2);
            String min = time.substring(2, 4);
            String sec = time.substring(4);
            UTC = String.format("%s:%s:%s", hour, min, sec);
        }

        if (!" ".equals(fields[2]) && !"".equals(fields[3])) {
            String lat = fields[2];
            String unit = fields[3];
            int i_dot = lat.indexOf(".");
            int max_length = Math.min(lat.length(), 11);
            String degree = lat.substring(0,i_dot-2);
            String min = lat.substring(i_dot-2,max_length);
            Latitude = String.format("%s°%s'%s",degree,min,unit);
        }

        if (!" ".equals(fields[4]) && !"".equals(fields[5])) {
            String longitude = fields[4];
            String unit = fields[5];
            int i_dot = longitude.indexOf(".");
            int max_length = Math.min(longitude.length(), 11);
            String degree = longitude.substring(0,i_dot-2);
            String min = longitude.substring(i_dot-2,max_length);
            Longitude = String.format("%s°%s'%s",degree,min,unit);
        }

        if (!" ".equals(fields[6])) {
            int status = Integer.parseInt(fields[6]);
            switch(status){
                case 0:
                    Status = "未定位";
                    break;
                case 1:
                    Status = "单点定位";
                    break;
                case 2:
                    Status = "伪距差分";
                    break;
                case 3:
                    Status = "无效PPS";
                    break;
                case 4:
                    Status = "固定解";
                    break;
                case 5:
                    Status = "浮点解";
                    break;
                case 6:
                    Status = "正在估算...";
                    break;
                default:
                    Status = "未知";
            }
        }

        if (!"".equals(fields[7])) {
            sat_num = Integer.parseInt(fields[7]);
        }

        if (!" ".equals(fields[9])) {
            double h1 = Double.parseDouble(fields[9]);
            double h2 = 0;
            if (!" ".equals(fields[11]))
                h2 = Double.parseDouble(fields[11]);
            BigDecimal bd = new BigDecimal(h1+h2);
            bd = bd.setScale(5, BigDecimal.ROUND_HALF_EVEN);
            H = bd.doubleValue();
        }

        if (!" ".equals(fields[13])) {
            age = fields[13]+"s";
        }

        return this;
    }

    public String getLatitude() {
        if (!"".equals(Latitude))return Latitude;
        return "--";
    }

    public String getLongitude() {
        if (!"".equals(Longitude))return Longitude;
        return "--";
    }

    public String getH() {
        if (H != -1)return H+"m";
        return "--";
    }

    public int getSat_num() {
        return sat_num;
    }

    public String getAge() {
        if (!"".equals(age))return age;
        return "--";
    }

    public String getUTC() {
        return UTC;
    }

    public String getStatus() {
        return Status;
    }
}
