package com.example.appmobile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

public class SessionManager {

    lateinit var pref : SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var con: Context
    var PRIVATE_MODE: Int = 0

    constructor(con: Context){
        this.con = con
        pref = con.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }

    companion object {
        val PREF_NAME: String = "Login_Preferences"
        val IS_LOGIN: String = "isLoggedIn"
        val KEY_IDUSER: String = "nutilizador"
        val KEY_USERNAME: String = "username"
        val KEY_EMAIL: String = "email"
        val KEY_LOCAL : String = "local"
        val KEY_CARGO : String = "cargo"
        val KEY_IMAGEM : String = "imagem"
        val KEY_NOTIFY : String = "notify"
        val KEY_TOKENFIREBASE : String = "token_firebase"
        val TIPO : String = "tipo"
    }

    fun createLoginSession(nutilizador: Int, username: String, email: String, local: Int, cargo: String, imagem: String, notify: Int, tipo: String){
        editor.putBoolean(IS_LOGIN, true)
        editor.putInt(KEY_IDUSER, nutilizador)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_EMAIL, email)
        editor.putInt(KEY_LOCAL, local)
        editor.putString(KEY_CARGO, cargo)
        editor.putString(KEY_IMAGEM, imagem)
        editor.putInt(KEY_NOTIFY, notify)
        editor.putString(TIPO, tipo)
        editor.commit()
    }

    fun checkLogin(){
        if(!this.isLoggedIn()){
            var i: Intent = Intent(con, Login::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            con.startActivity(i)
        }
    }

    fun getUserDetails() : HashMap<String, String>{
        var user: Map<String, String> = HashMap<String, String>()
        (user as HashMap).put(KEY_USERNAME, pref.getString(KEY_USERNAME, null)!!)
        (user as HashMap).put(KEY_CARGO, pref.getString(KEY_CARGO, null)!!)
        return user
    }

    fun getEstabelecimento() : Int{
        return pref.getInt(KEY_LOCAL, 0)
    }

    fun getTipo() : String{
        return pref.getString(TIPO, "")!!
    }

    fun getUser() : Int{
        return pref.getInt(KEY_IDUSER, 0)
    }

    fun getNotify() : Int{
        return pref.getInt(KEY_NOTIFY, 0)
    }

    fun setNotify(n : Int){
        editor.putInt(KEY_NOTIFY, n)
        editor.commit()
    }

    fun getImagem() : String{
        return pref.getString(KEY_IMAGEM, "")!!
    }

    fun getNome() : String{
        return pref.getString(KEY_USERNAME, "")!!
    }

    fun setNome(nome: String){
        editor.putString(KEY_USERNAME, nome)
        editor.commit()
    }

    fun getCargo() : String{
        return pref.getString(KEY_CARGO, "")!!
    }

    fun setImagem(img : String){
        Log.i("Opcoes Session", img)
        editor.putString(KEY_IMAGEM, img)
        editor.commit()
    }

    fun setTokenFirebase(token : String){
        editor.putString(KEY_TOKENFIREBASE, token)
        editor.commit()
    }

    fun getTokenFirebase() : String{
        return pref.getString(KEY_TOKENFIREBASE, "")!!
    }

    fun setCargo(cargo : String){
        editor.putString(KEY_CARGO, cargo)
        editor.commit()
    }

    fun getEmail() : String{
        return pref.getString(KEY_EMAIL, "")!!
    }

    fun LogoutUser(){
        editor.clear()
        editor.commit()
        var i: Intent = Intent(con, Login::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        con.startActivity(i)
    }

    fun isLoggedIn() : Boolean {
        return pref.getBoolean(IS_LOGIN, false)
    }


}