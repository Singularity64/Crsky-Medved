package cz.ceskymedvidek.app
import android.app.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import java.util.Locale

data class Word(val name:String,val icon:String,val world:String)

class MainActivity:Activity(),TextToSpeech.OnInitListener{
 private lateinit var root:LinearLayout
 private lateinit var tts:TextToSpeech
 private val prefs by lazy{getSharedPreferences("medvidek",MODE_PRIVATE)}
 private var stars:Int get()=prefs.getInt("stars",0); set(v){prefs.edit().putInt("stars",v).apply()}
 private val words=listOf(
 Word("kráva","🐮","Farma"),Word("kůň","🐴","Farma"),Word("prase","🐷","Farma"),Word("ovce","🐑","Farma"),Word("koza","🐐","Farma"),Word("slepice","🐔","Farma"),Word("kachna","🦆","Farma"),Word("králík","🐰","Farma"),
 Word("pes","🐶","Domeček"),Word("kočka","🐱","Domeček"),Word("dům","🏠","Domeček"),Word("postel","🛏️","Domeček"),Word("židle","🪑","Domeček"),Word("dveře","🚪","Domeček"),Word("klíč","🔑","Domeček"),Word("hodiny","🕐","Domeček"),
 Word("strom","🌳","Les"),Word("houba","🍄","Les"),Word("liška","🦊","Les"),Word("jelen","🦌","Les"),Word("ježek","🦔","Les"),Word("veverka","🐿️","Les"),Word("list","🍃","Les"),Word("květina","🌼","Les"),
 Word("auto","🚗","Město"),Word("autobus","🚌","Město"),Word("kolo","🚲","Město"),Word("semafor","🚦","Město"),Word("taxi","🚕","Město"),Word("vlak","🚆","Město"),Word("sanitka","🚑","Město"),Word("policie","🚓","Město"),
 Word("jablko","🍎","Obchod"),Word("banán","🍌","Obchod"),Word("chléb","🍞","Obchod"),Word("mléko","🥛","Obchod"),Word("sýr","🧀","Obchod"),Word("vejce","🥚","Obchod"),Word("mrkev","🥕","Obchod"),Word("jahoda","🍓","Obchod"),
 Word("stan","⛺","Výlet"),Word("batoh","🎒","Výlet"),Word("mapa","🗺️","Výlet"),Word("hora","⛰️","Výlet"),Word("slunce","☀️","Výlet"),Word("mrak","☁️","Výlet"),Word("loď","⛵","Výlet"),Word("fotoaparát","📷","Výlet"))
 private val worlds=listOf("Domeček" to "🏠","Farma" to "🐄","Les" to "🌲","Město" to "🏙️","Obchod" to "🛒","Výlet" to "🎒")
 override fun onCreate(b:Bundle?){super.onCreate(b);tts=TextToSpeech(this,this);splash()}
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("cs","CZ");tts.setSpeechRate(.82f)}}
 private fun base(){root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,42,28,28);background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(220,246,255),Color.rgb(247,238,255)))};setContentView(ScrollView(this).apply{addView(root)})}
 private fun text(s:String,size:Int=22,bold:Boolean=false){root.addView(TextView(this).apply{text=s;textSize=size.toFloat();gravity=Gravity.CENTER;setTextColor(Color.rgb(31,61,112));if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(8,10,8,10)})}
 private fun btn(s:String,a:()->Unit){root.addView(Button(this).apply{text=s;textSize=20f;setAllCaps(false);setTextColor(Color.rgb(25,55,100));background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(2,Color.rgb(190,218,244))};setPadding(18,16,18,16);setOnClickListener{a()}},LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})}
 private fun say(s:String)=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"cz")
 private fun splash(){base();root.gravity=Gravity.CENTER;val b=TextView(this).apply{text="🧸";textSize=112f;gravity=Gravity.CENTER;alpha=0f;scaleX=.4f;scaleY=.4f};root.addView(b);text("Český medvídek",34,true);b.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(OvershootInterpolator());Handler(Looper.getMainLooper()).postDelayed({home()},2200)}
 private fun home(){base();text("Český medvídek",32,true);text("🧸",82);text("Ahoj! Co si dnes zahrajeme?",20);text("⭐ "+stars+"     🏆 Úroveň "+(1+stars/20),18,true);btn("📚  UČÍM SE"){worlds(false)};btn("🎯  NAJDI OBRÁZEK"){worlds(true)};btn("🎁  ODMĚNY"){rewards()};btn("🔊  POSLOUCHEJ A OPAKUJ"){listen()}}
 private fun worlds(q:Boolean){base();text(if(q)"Vyber svět pro hru" else "Vyber svět",28,true);worlds.forEach{w->btn(w.second+"   "+w.first){if(q)quiz(w.first)else learn(w.first,0)}};btn("⌂  Domů"){home()}}
 private fun learn(world:String,i:Int){val ws=words.filter{it.world==world};val x=ws[i%ws.size];base();text(world,25,true);text(x.icon,112);text(x.name.uppercase(),34,true);btn("🔊  Poslechni si"){say(x.name)};btn("Další  ➜"){learn(world,i+1)};btn("🎯  Procvičit"){quiz(world)};btn("⌂  Domů"){home()}}
 private fun quiz(world:String){val pool=words.filter{it.world==world};val target=pool.random();val choices=(pool.filter{it!=target}.shuffled().take(3)+target).shuffled();base();text("Najdi správný obrázek",25,true);text("Najdi: "+target.name,30,true);btn("🔊  Přehrát zadání"){say("Najdi "+target.name)};choices.forEach{c->btn(c.icon+"   "+c.name){if(c==target){stars+=1;say("Výborně");Toast.makeText(this,"⭐ +1 hvězdička",Toast.LENGTH_SHORT).show();quiz(world)}else{say("Zkus to ještě jednou");Toast.makeText(this,"Zkus to ještě jednou",Toast.LENGTH_SHORT).show()}}};btn("⌂  Domů"){home()}}
 private fun listen(){val x=words.random();base();text("Poslouchej a opakuj",27,true);text(x.icon,110);text(x.name,34,true);btn("🔊  Přehrát slovo"){say(x.name)};btn("Další slovo"){listen()};btn("⌂  Domů"){home()}}
 private fun rewards(){base();text("Moje odměny",30,true);text("⭐ "+stars,42,true);val r=listOf(5 to "🎈 Balónek",10 to "🧢 Čepice",20 to "🧸 Plyšák",35 to "👓 Brýle",50 to "🏆 Zlatý pohár",100 to "👑 Koruna");r.forEach{(n,name)->text(if(stars>=n)"✅ "+name else "🔒 "+name+"  •  "+n+" ⭐",20)};btn("🎯  Získat hvězdičky"){worlds(true)};btn("⌂  Domů"){home()}}
 override fun onBackPressed(){home()}
 override fun onDestroy(){tts.stop();tts.shutdown();super.onDestroy()}
}