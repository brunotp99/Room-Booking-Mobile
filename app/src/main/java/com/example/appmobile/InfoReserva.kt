package com.example.appmobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.example.appmobile.databinding.ActivityInfoReservaBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class InfoReserva : AppCompatActivity() {

    lateinit var session : SessionManager
    private lateinit var binding: ActivityInfoReservaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoReservaBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Para remover um sombrado atraz do menu */
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.background = null
        /* Desativar o icon escondido que alinha o menu */
        bottomNavigationView.menu.getItem(2).isEnabled = false

        /* Executar botao QRCODE */
        val btnQrcode = findViewById<FloatingActionButton>(R.id.fab)
        btnQrcode.setOnClickListener {
            var i = Intent(applicationContext, QRcodeActivity::class.java)
            startActivity(i)
        }

        /* Botoes do Menu */
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    val iHome = Intent(applicationContext, Home::class.java)
                    startActivity(iHome)
                }
                R.id.page_2 -> {
                    val iPesquisa = Intent(applicationContext, pesquisaSalas::class.java)
                    startActivity(iPesquisa)
                }
                R.id.page_3 -> {
                    val iMinhas = Intent(applicationContext, MinhasReservas::class.java)
                    startActivity(iMinhas)
                }
                R.id.page_4 -> {
                    val Opcoes = Intent(applicationContext, Opcoes::class.java)
                    startActivity(Opcoes)
                }
            }
            return@setOnItemSelectedListener true
        }

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener {menuItem ->
            when (menuItem.itemId) {
                R.id.notify -> {
                    var dialog = NotificacoesFragment()
                    dialog.show(supportFragmentManager, "TAG")
                    true
                }
                else -> false
            }

        }

        /* Voltar */
        val voltarBtn = findViewById<TextView>(R.id.goback)
        voltarBtn.setOnClickListener {
            finish()
        }

        /* Obter dados de intents */
        val bundle = intent.extras
        val horas = bundle!!.getString("horas")
        val datas = bundle.getString("data")
        val sala = bundle.getString("sala")
        val requisitante = bundle.getString("requisitante")
        val cargo = bundle.getString("cargo")
        val imagem = bundle.getString("imagem")
        val descricao = bundle.getString("descricao")

        /* Atribuir os dados aos objetos XML */
        binding.txtUtilizador.text = requisitante
        binding.txtCargoUtilizador.text = cargo
        binding.hora.text = horas
        binding.data.text = datas
        binding.desc.text = descricao
        binding.txtSala.text = sala
        val userImg = findViewById<ShapeableImageView>(R.id.imgPerfil)
        Picasso.get().load(appSettings.URLfindImage + imagem).resize(50, 50)
            .centerCrop()
            .placeholder(R.drawable.user)
            .error(R.drawable.user)
            .into(userImg);

    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val menuItem = bottomNavigationView.menu.getItem(0)
        if (!menuItem.isChecked) {
            menuItem.isChecked = true
        }
    }
}