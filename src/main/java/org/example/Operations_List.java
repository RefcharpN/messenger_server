package org.example;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Operations_List {

    Map<String, Callable<String>> commands;
    private String url;
    private String user;
    private String password;

    public Operations_List(String adr, String pass, JSONObject json) {

        this.url = String.format("jdbc:postgresql://%s:5432/postgres", adr);
        this.user = "postgres";
        this.password = String.format("%s", pass);

        this.commands = new HashMap<String, Callable<String>>();
        //TODO:здесь список операций
        this.commands.put("1", () -> login(json));
//        this.commands.put("2", () -> phone_check(json));
//        this.commands.put("3", () -> registration_user(json));


    }

    public String processing(String cmd) throws Exception {
        return commands.get(cmd).call();
    }

    public static String generateTOTP(String base32Key) throws Exception {
        long counter = (System.currentTimeMillis()  / 1000) / 30;
        byte[] key = new Base32().decode(base32Key);

        SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);

        byte[] counterBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            counterBytes[7 - i] = (byte) (counter >> (8 * i));
        }

        byte[] hash = mac.doFinal(counterBytes);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24 | (hash[offset + 1] & 0xFF) << 16 | (hash[offset + 2] & 0xFF) << 8 | (hash[offset + 3] & 0xFF));

        int otp = binary % (int) Math.pow(10, 6);
        return String.format("%0" + 6 + "d", otp);
    }

    public String login(JSONObject json) {
        System.out.println("запрос на вход");
        JSONObject json_out = new JSONObject();
//проверка токена
        String query_token = String.format("select usertoken FROM messanger.login_users  WHERE UserLogin = '%s' ;", json.getString("LOGIN"));

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query_token))
        {

            if (rs.next()) {
                if(!Objects.equals(json.getString("TOKEN"), generateTOTP(rs.getString(1))))
                {
                    System.out.println("токен не прошёл проверку");
                    json_out.put("EXIST", "0");
                    return json_out.toString();
                }
                else
                {
                    System.out.println("токен прошёл проверку");

                }

            }
            else
            {
                System.out.println("пользователь не найден");
                json_out.put("EXIST", "0");
                return json_out.toString();
            }


        } catch (SQLException ex) {
            System.out.println("ошибка в функции login - SQLException - класс Operation_list" + ex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("проверка логина");

//проверка логина
        String query_login = String.format("SELECT (UserPassword = crypt('%s', UserPassword)) \n" +
                "\n" +
                "    AS password_match \n" +
                "\n" +
                "FROM messanger.login_users  \n" +
                "\n" +
                "WHERE UserLogin = '%s' ;", json.getString("PASSWORD"), json.getString("LOGIN"));

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query_login))
        {

            if (rs.next()) {
                if(Objects.equals(rs.getString(1), "t"))
                {
                    json_out.put("EXIST", "1");
                }
                else
                {
                    System.out.println("пароль не совпадает");
                }
            }
            else
            {
                System.out.println("пользователь не найден");
            }

            return json_out.toString();

        } catch (SQLException ex) {
            System.out.println( "ошибка в функции login - SQLException - класс Operation_list" + ex);
        }
        json_out.put("EXIST", "0");
        return json_out.toString();
    }

//    public String phone_check(JSONObject json) {
//        System.out.println("запрос проверки телефона");
//        JSONObject json_out = new JSONObject();
//
//        String query = String.format("select * from bank_test.phone_check('%s')", json.getString("PHONE"));
//
//        try (Connection con = DriverManager.getConnection(url, user, password);
//             Statement st = con.createStatement();
//             ResultSet rs = st.executeQuery(query))
//        {
//
//            if (rs.next()) {
//                json_out.put("EXIST", rs.getString(1));
//            }
//
//            return json_out.toString();
//
//        } catch (SQLException ex)
//        {
//            Server.logger_error.log(Level.INFO, "ошибка в функции phone_check - SQLException - класс Operation_list");
//        }
//
//
//        json_out.put("EXIST", "0");
//        return json_out.toString();
//    }

//    public String registration_user(JSONObject json)
//    {
//        System.out.println("регистрация клиента");
//        JSONObject json_out = new JSONObject();
//
//        String query = String.format("select * from bank_test.client_register('%s', '%s', '%s', '%s', '%s', %s, %s, '%s', '%s')",
//                json.getString("phone"),
//                json.getString("password"),json.getString("lname"),
//                json.getString("fname"),json.getString("mname"),
//                json.getString("series"),json.getString("number"),
//                json.getString("date"),json.getString("department"));
//
//
//        try (Connection con = DriverManager.getConnection(url, user, password);
//             Statement st = con.createStatement();
//             ResultSet rs = st.executeQuery(query))//TODO проверка ошибок выполнения
//        {
////
////            if (rs.next()) {
////                json_out.put("EXIST", rs.getString(1));
////            }
////            return json_out.toString();
//
//        } catch (SQLException ex) {
//            Server.logger_error.log(Level.INFO, "ошибка в функции registration_user - SQLException - класс Operation_list");
//            json_out.put("register_status", "1");
//            return json_out.toString();
//        }
//
//        json_out.put("register_status", "0");
//        return json_out.toString();
//    }
}
