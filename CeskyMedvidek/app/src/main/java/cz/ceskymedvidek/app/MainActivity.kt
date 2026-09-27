package cz.ceskymedvidek.app
import android.Manifest
import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import java.text.Normalizer
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Word(val name:String,val icon:String,val world:String)
data class StickerReward(val key:String,val icon:String,val name:String)
data class RewardGrant(val praise:String,val reward:StickerReward,val isNew:Boolean,val collectionCompleted:Boolean)

class TwinkleStarsView(context:Context,private val filled:Int,private val total:Int=5):View(context){
 private val density=resources.displayMetrics.density
 private val onPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(255,193,7);style=Paint.Style.FILL}
 private val offPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(205,207,216);style=Paint.Style.FILL}
 private var phase=0f
 private val animator=ValueAnimator.ofFloat(0f,1f).apply{
  duration=1100
  repeatCount=ValueAnimator.INFINITE
  repeatMode=ValueAnimator.RESTART
  addUpdateListener{phase=it.animatedValue as Float;invalidate()}
 }
 init{setLayerType(LAYER_TYPE_SOFTWARE,null);animator.start()}
 private fun star(cx:Float,cy:Float,r:Float):Path{
  val p=Path()
  for(i in 0 until 10){
   val a=-PI/2+i*PI/5
   val rr=if(i%2==0)r else r*.46f
   val x=cx+(cos(a)*rr).toFloat()
   val y=cy+(sin(a)*rr).toFloat()
   if(i==0)p.moveTo(x,y)else p.lineTo(x,y)
  }
  p.close();return p
 }
 override fun onMeasure(w:Int,h:Int){
  val ww=(total*39*density).toInt()
  val hh=(42*density).toInt()
  setMeasuredDimension(resolveSize(ww,w),resolveSize(hh,h))
 }
 override fun onDraw(c:Canvas){
  super.onDraw(c)
  val step=width.toFloat()/total
  val baseR=(16*density).coerceAtMost(height*.38f)
  for(i in 0 until total){
   val active=i<filled
   val pulse=((sin((phase*2*PI)+(i*.9))+1.0)/2.0).toFloat()
   val r=if(active)baseR*(.94f+.08f*pulse)else baseR*.9f
   val paint=if(active)onPaint else offPaint
   if(active){
    paint.alpha=(210+45*pulse).toInt()
    paint.setShadowLayer(7*density+4*density*pulse,0f,0f,Color.rgb(255,224,80))
   }else{
    paint.alpha=210
    paint.clearShadowLayer()
   }
   c.drawPath(star(step*(i+.5f),height/2f,r),paint)
   if(active&&pulse>.72f){
    val sparkle=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;alpha=(160*pulse).toInt()}
    val x=step*(i+.5f)+r*.75f
    val y=height/2f-r*.72f
    c.drawCircle(x,y,2.2f*density,sparkle)
   }
  }
 }
 override fun onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow()}
}

class MainActivity:Activity(),TextToSpeech.OnInitListener{
 companion object{private const val REQ_RECORD_AUDIO=41}
 private lateinit var root:LinearLayout
 private lateinit var tts:TextToSpeech
 private var speechRecognizer:SpeechRecognizer?=null
 private var speechStatus:TextView?=null
 private var speechNextButton:Button?=null
 private var expectedSpeechWord:String?=null
 private var expectedSpeechWorld:String?=null
 private var speechWordPassed=false
 private var speechScreenActive=false
 private var resumeListeningAfterTts:String?=null
 private val speechHandler=Handler(Looper.getMainLooper())

 private val prefs by lazy{getSharedPreferences("medvidek",MODE_PRIVATE)}
 private var stars:Int get()=prefs.getInt("stars",0); set(v){prefs.edit().putInt("stars",v).apply()}
 private val praisePhrases=listOf(
 "Výborně!",
 "Paráda!",
 "Skvěle!",
 "Perfektní!",
 "To se ti povedlo!",
 "Jsi šikula!",
 "Super práce!",
 "Máš to!",
 "Nádhera!",
 "Jen tak dál!"
 )
 private val stickerRewards=listOf(
  StickerReward("beri","","Beri"),
  StickerReward("balonek","","Balónek"),
  StickerReward("hvezda","","Hvězdička"),
  StickerReward("duha","","Duha"),
  StickerReward("srdce","","Srdíčko"),
  StickerReward("korunka","","Korunka"),
  StickerReward("pohar","","Pohár"),
  StickerReward("raketa","","Raketa"),
  StickerReward("jednorozec","","Jednorožec"),
  StickerReward("darek","","Dárek")
 )
 private val adventureTreasures=listOf(
  StickerReward("auto","","Závodní auto"),
  StickerReward("robot","","Robot"),
  StickerReward("dino","","Dinosaurus"),
  StickerReward("raketa2","","Raketa"),
  StickerReward("pirat","","Pirátský poklad"),
  StickerReward("stit","","Hrdinský štít")
 )
 private val magicTreasures=listOf(
  StickerReward("jednorozec2","","Jednorožec"),
  StickerReward("korunka2","","Korunka"),
  StickerReward("hvezdna_hulka","","Kouzelná hůlka"),
  StickerReward("motyl","","Kouzelný motýl"),
  StickerReward("diamant","","Diamant"),
  StickerReward("duha2","","Duhový poklad")
 )
 private val animalTreasures=listOf(
  StickerReward("panda","","Panda"),
  StickerReward("lev","","Lev"),
  StickerReward("lisak","","Lišák"),
  StickerReward("delfin","","Delfín"),
  StickerReward("pejsek","","Pejsek"),
  StickerReward("tucnak","","Tučňák")
 )
 private val creativeTreasures=listOf(
  StickerReward("paleta","","Malířská paleta"),
  StickerReward("kytara","","Kytara"),
  StickerReward("puzzle","","Puzzle"),
  StickerReward("kniha","","Kouzelná kniha"),
  StickerReward("mic","","Míč"),
  StickerReward("foto","","Fotoaparát")
 )
 private var pendingChests:Int
  get()=prefs.getInt("pending_chests",0)
  set(v){prefs.edit().putInt("pending_chests",v.coerceAtLeast(0)).apply()}
 private fun childProfile()=prefs.getString("child_profile","neutral")?:"neutral"
 private fun rewardTheme()=prefs.getString("reward_theme","")?:""
 private fun treasurePool():List<StickerReward>{
  return when(rewardTheme()){
 "adventure"->adventureTreasures
 "magic"->magicTreasures
 "animals"->animalTreasures
 "creative"->creativeTreasures
   else->when(childProfile()){
 "boy"->adventureTreasures
 "girl"->magicTreasures
    else->animalTreasures
   }
  }
 }
 private fun treasureCount(key:String)=prefs.getInt("treasure_"+key,0)
 private fun vocabularyTreasureCount():Int=(adventureTreasures+magicTreasures+animalTreasures+creativeTreasures).distinctBy{it.key}.sumOf{treasureCount(it.key)}
 private val vocabulary=mapOf(
 "Domeček" to listOf("dům","panelák","pokoj","postel","polštář","deka","stůl","židle","okno","dveře"),
 "Domácnost" to listOf("kuchyně","koupelna","ložnice","chodba","balkon","zahrada","garáž","střecha","zeď","podlaha","strop","schody","klíč","zámek","křeslo","police","koberec","zrcadlo","hodiny","obraz","záclona","peřina","ručník","mýdlo","kartáček","hřeben","vysavač","koště","lopatka","pračka","lednice","trouba","sporák","konvice","hrnek","talíř","lžíce"),
 "Zvířata" to listOf("pes","kočka","králík","křeček","morče","papoušek","ryba","želva","kůň","kráva","prase","ovce","koza","slepice","kohout","kachna","husa","krocan","osel","jelen","srna","liška","vlk","medvěd","ježek","veverka","zajíc","divočák","myš","krtek","lev","tygr","slon","žirafa","zebra","opice","gorila","klokan","panda","hroch","nosorožec","krokodýl","had","ještěrka","žába","čáp","sova","orel","tučňák","delfín"),
 "Jídlo" to listOf("chléb","rohlík","houska","máslo","sýr","šunka","salám","vejce","mléko","jogurt","tvaroh","smetana","polévka","maso","kuře","ryba","rýže","těstoviny","brambory","knedlík","omáčka","salát","pizza","hamburger","párek","kaše","mouka","cukr","sůl","pepř","med","džem","čokoláda","sušenka","dort","zmrzlina","bonbón","ořech","snídaně","oběd","večeře","svačina","voda","čaj","kakao","džus","limonáda","hlad","žízeň","chuť"),
 "Ovoce a zelenina" to listOf("jablko","hruška","banán","pomeranč","mandarinka","citron","limetka","grep","broskev","meruňka","švestka","třešeň","višeň","jahoda","malina","ostružina","borůvka","rybíz","angrešt","hroznové víno","meloun","ananas","mango","kiwi","kokos","avokádo","rajče","okurka","paprika","mrkev","petržel","celer","ředkvička","řepa","zelí","kapusta","salát","špenát","brokolice","květák","hrášek","fazole","čočka","kukuřice","cibule","česnek","dýně","cuketa","lilek","houba"),
 "Tělo" to listOf("hlava","vlasy","čelo","oko","obočí","řasa","ucho","nos","tvář","pusa","ret","zub","jazyk","brada","krk","rameno","ruka","loket","zápěstí","dlaň","prst","palec","nehet","hrudník","břicho","záda","bok","noha","koleno","kotník","pata","chodidlo","kůže","kost","sval","srdce","mozek","krev","dech","hlas","slza","pot","úsměv","bolest","zdraví","nemoc","teplota","kašel","rýma","spánek"),
 "Oblečení" to listOf("tričko","košile","halenka","mikina","svetr","bunda","kabát","vesta","kalhoty","džíny","kraťasy","sukně","šaty","pyžamo","spodní prádlo","ponožka","punčocha","boty","tenisky","sandály","holínky","bačkory","čepice","klobouk","kšiltovka","šála","rukavice","pásek","kravata","motýlek","kapsa","knoflík","zip","kapuce","deštník","batoh","kabelka","kufr","brýle","sluneční brýle","hodinky","prsten","náhrdelník","náramek","náušnice","velikost","barva","látka","vlna","bavlna"),
 "Doprava" to listOf("auto","autobus","trolejbus","tramvaj","vlak","metro","taxi","kolo","koloběžka","motorka","nákladní auto","dodávka","traktor","sanitka","policejní auto","hasičské auto","loď","člun","trajekt","letadlo","vrtulník","raketa","silnice","ulice","dálnice","most","tunel","křižovatka","semafor","přechod","chodník","zastávka","nádraží","letiště","přístav","parkoviště","garáž","volant","kolo auta","pneumatika","motor","sedadlo","pás","jízdenka","lístek","řidič","cestující","cesta","výlet","dopravní značka"),
 "Město" to listOf("město","vesnice","dům","panelák","škola","školka","nemocnice","lékárna","obchod","supermarket","pekárna","restaurace","kavárna","pošta","banka","knihovna","kino","divadlo","muzeum","kostel","radnice","policie","hasiči","park","hřiště","stadion","bazén","zoo","náměstí","ulice","chodník","lavička","kašna","socha","věž","most","řeka","semafor","přechod","zastávka","nádraží","parkoviště","popelnice","kontejner","výloha","vchod","výtah","eskalátor","mapa","adresa"),
 "Příroda" to listOf("strom","keř","tráva","květina","list","větev","kořen","kmen","kůra","semínko","plod","les","louka","pole","zahrada","park","hora","kopec","skála","jeskyně","údolí","řeka","potok","jezero","rybník","moře","oceán","ostrov","pláž","písek","kámen","hlína","bahno","voda","vodopád","pramen","obloha","slunce","měsíc","hvězda","mrak","déšť","sníh","led","vítr","bouřka","blesk","duha","stín","příroda"),
 "Škola" to listOf("škola","třída","učitel","učitelka","žák","žákyně","kamarád","lavice","tabule","křída","fix","sešit","učebnice","kniha","papír","tužka","pero","pastelka","guma","pravítko","nůžky","lepidlo","aktovka","penál","úkol","otázka","odpověď","písmeno","slovo","věta","číslo","počítání","čtení","psaní","kreslení","zpěv","hudba","tělocvik","přestávka","zvonění","oběd","jídelna","družina","výlet","známka","pochvala","chyba","nápad","učení","prázdniny"),
 "Hračky" to listOf("hračka","panenka","medvídek","míč","kostka","stavebnice","puzzle","pexeso","autíčko","vláček","letadélko","loďka","robot","drak","balónek","bublifuk","houpačka","skluzavka","pískoviště","koloběžka","tříkolka","kolo","brusle","lyže","sáňky","pastelky","omalovánky","plastelína","loutka","maňásek","domeček","garáž","dráha","figurky","karty","kostky","hra","schovávaná","honička","skákání","stavění","kreslení","malování","zpívání","tanec","pohádka","dárek","oslava","narozeniny","zábava"),
 "Barvy a tvary" to listOf("červená","modrá","zelená","žlutá","oranžová","fialová","růžová","hnědá","černá","bílá","šedá","zlatá","stříbrná","světlá","tmavá","barevná","kruh","čtverec","trojúhelník","obdélník","ovál","hvězda","srdce","kříž","čára","bod","roh","strana","koule","kostka","válec","kužel","malý","velký","dlouhý","krátký","široký","úzký","vysoký","nízký","kulatý","hranatý","rovný","křivý","plný","prázdný","stejný","jiný","uprostřed","okraj"),
 "Čísla a čas" to listOf("nula","jedna","dva","tři","čtyři","pět","šest","sedm","osm","devět","deset","jedenáct","dvanáct","třináct","čtrnáct","patnáct","šestnáct","sedmnáct","osmnáct","devatenáct","dvacet","první","druhý","třetí","poslední","málo","hodně","více","méně","všechno","nic","čas","hodina","minuta","sekunda","den","noc","ráno","dopoledne","poledne","odpoledne","večer","dnes","včera","zítra","týden","měsíc","rok","brzy","pozdě"),
 "Rodina a lidé" to listOf("máma","tatínek","rodič","dítě","syn","dcera","bratr","sestra","babička","dědeček","vnuk","vnučka","teta","strýc","bratranec","sestřenice","rodina","kamarád","kamarádka","soused","sousedka","miminko","kluk","holka","muž","žena","pán","paní","člověk","jméno","věk","domov","návštěva","svatba","narozeniny","pomoc","pozdrav","ahoj","dobrý den","prosím","děkuji","promiň","ano","ne","spolu","sám","radost","smutek","láska","přátelství"),
 "Povolání" to listOf("lékař","lékařka","zdravotní sestra","zubař","učitel","učitelka","policista","policistka","hasič","hasička","řidič","řidička","kuchař","kuchařka","pekař","pekařka","prodavač","prodavačka","pošťák","farmář","zahradník","mechanik","stavitel","zedník","truhlář","malíř","uklízeč","kadeřník","fotograf","herec","herečka","zpěvák","zpěvačka","hudebník","sportovec","trenér","pilot","strojvedoucí","průvodčí","kapitán","vědec","programátor","veterinář","záchranář","knihovník","číšník","servírka","řezník","cukrář","práce"),
 "Sport" to listOf("sport","fotbal","hokej","tenis","basketbal","volejbal","házená","florbal","baseball","golf","plavání","běh","skok","cyklistika","lyžování","bruslení","gymnastika","tanec","judo","karate","box","míč","branka","gól","hřiště","stadion","bazén","raketa","síť","hokejka","puk","helma","dres","trenýrky","tenisky","kopačky","medaile","pohár","závod","tým","hráč","trenér","rozhodčí","výhra","prohra","remíza","start","cíl","rychlost","síla"),
 "Slovesa" to listOf("být","mít","dělat","jít","jet","běžet","chodit","skákat","sedět","stát","ležet","spát","vstát","jíst","pít","vařit","péct","krájet","míchat","umýt","čistit","uklízet","otevřít","zavřít","dát","vzít","nést","držet","pustit","hodit","chytit","kopnout","vidět","dívat se","slyšet","poslouchat","mluvit","říct","ptát se","odpovědět","číst","psát","kreslit","počítat","učit se","hrát si","zpívat","tančit","smát se","plakat"),
 "Vlastnosti" to listOf("dobrý","špatný","hezký","ošklivý","nový","starý","mladý","rychlý","pomalý","silný","slabý","lehký","těžký","teplý","studený","horký","čistý","špinavý","suchý","mokrý","měkký","tvrdý","hladký","drsný","sladký","slaný","kyselý","hořký","hlasitý","tichý","veselý","smutný","hodný","zlý","chytrý","hloupý","statečný","opatrný","unavený","zdravý","nemocný","hladový","žíznivý","plný","prázdný","otevřený","zavřený","blízko","daleko","společný"),
 "Počasí a roční doby" to listOf("počasí","jaro","léto","podzim","zima","leden","únor","březen","duben","květen","červen","červenec","srpen","září","říjen","listopad","prosinec","slunce","mrak","déšť","kapka","sníh","vločka","led","kroupy","vítr","vánek","bouřka","blesk","hrom","duha","mlha","mráz","teplo","horko","chlad","stín","světlo","tma","obloha","teplota","deštník","pláštěnka","kaluž","sněhulák","sáňky","rukavice","čepice","prázdniny","roční období"),
 "Výlet a cestování" to listOf("výlet","cesta","dovolená","batoh","kufr","taška","mapa","kompas","stan","spacák","karimatka","baterka","láhev","svačina","fotoaparát","fotka","jízdenka","letenka","pas","hotel","pokoj","recepce","kemp","chata","hrad","zámek","rozhledna","muzeum","zoo","hora","les","jezero","moře","pláž","ostrov","letiště","nádraží","přístav","vlak","autobus","letadlo","loď","turista","průvodce","směr","vlevo","vpravo","rovně","návrat","zážitek")
 )
 private val worldIcons=mapOf(
 "Domeček" to "",
 "Domácnost" to "",
 "Zvířata" to "",
 "Jídlo" to "",
 "Ovoce a zelenina" to "",
 "Tělo" to "",
 "Oblečení" to "",
 "Doprava" to "",
 "Město" to "",
 "Příroda" to "",
 "Škola" to "",
 "Hračky" to "",
 "Barvy a tvary" to "",
 "Čísla a čas" to "",
 "Rodina a lidé" to "",
 "Povolání" to "",
 "Sport" to "",
 "Slovesa" to "",
 "Vlastnosti" to "",
 "Počasí a roční doby" to "",
 "Výlet a cestování" to ""
 )
 private val words by lazy { vocabulary.flatMap { (world,names) -> names.map { Word(it, worldIcons[world] ?: "", world) } } }
 private val worldList by lazy { vocabulary.keys.map { it to (worldIcons[it] ?: "") } }
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  immersiveUi()
  tts=TextToSpeech(this,this)
  home()
 }
 private fun immersiveUi(){
  window.statusBarColor=Color.TRANSPARENT
  window.navigationBarColor=Color.TRANSPARENT
  window.decorView.systemUiVisibility=
   View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
   View.SYSTEM_UI_FLAG_FULLSCREEN or
   View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
   View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
   View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
   View.SYSTEM_UI_FLAG_LAYOUT_STABLE
 }
 override fun onInit(s:Int){
  if(s==TextToSpeech.SUCCESS){
   tts.language=Locale("cs","CZ")
   tts.setSpeechRate(.82f)
   tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
    override fun onStart(utteranceId:String?){}
    override fun onDone(utteranceId:String?){
     if(utteranceId=="word_playback"){
      val word=resumeListeningAfterTts
      resumeListeningAfterTts=null
      if(word!=null)runOnUiThread{
       if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
      }
     }
    }
    override fun onError(utteranceId:String?){
     if(utteranceId=="word_playback"){
      val word=resumeListeningAfterTts
      resumeListeningAfterTts=null
      if(word!=null)runOnUiThread{
       if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
      }
     }
    }
   })
  }
 }
 private fun base(){
  speechScreenActive=false
  resumeListeningAfterTts=null
  cancelListening()
  val frame=FrameLayout(this).apply{
   setBackgroundColor(Color.rgb(247,250,253))
  }
  frame.addView(View(this).apply{
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(244,249,255),Color.rgb(255,250,242)))
  },FrameLayout.LayoutParams(-1,-1))
  root=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER_HORIZONTAL
   setPadding(22,34,22,42)
   setBackgroundColor(Color.TRANSPARENT)
  }
  val scroll=ScrollView(this).apply{
   isFillViewport=true
   clipToPadding=false
   addView(root,ViewGroup.LayoutParams(-1,-2))
  }
  frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))
  setContentView(frame)
 }
 private fun text(s:String,size:Int=22,bold:Boolean=false){
  root.addView(TextView(this).apply{
   text=s;textSize=size.toFloat();gravity=Gravity.CENTER
   setTextColor(Color.rgb(82,43,25))
   setShadowLayer(5f,0f,2f,Color.WHITE)
   if(bold)setTypeface(typeface,Typeface.BOLD)
   setPadding(8,10,8,10)
  })
 }
 private fun speechInfo(s:String=" Mikrofon je zapnutý. Řekni slovo."){
  speechStatus=TextView(this).apply{text=s;textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.rgb(31,61,112));setPadding(8,8,8,12)}
  root.addView(speechStatus)
 }
 private fun bigBtn(title:String,subtitle:String,color:Int,a:()->Unit){
  root.addView(Button(this).apply{
   text=title+"\n"+subtitle
   textSize=21f
   setAllCaps(false)
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   setTypeface(typeface,Typeface.BOLD)
   background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(color,Color.argb(255,
    ((Color.red(color)+255)/2),((Color.green(color)+255)/2),((Color.blue(color)+255)/2)))).apply{
     cornerRadius=44f;setStroke(3,Color.argb(215,255,255,255))
   }
   elevation=10f
   setPadding(18,24,18,24)
   minHeight=140
   setOnClickListener{a()}
  },LinearLayout.LayoutParams(-1,-2).apply{setMargins(10,11,10,11)})
 }
 private fun btn(s:String,a:()->Unit){
  root.addView(Button(this).apply{
   text=s;textSize=19f;setAllCaps(false);setTextColor(Color.rgb(92,45,24));setTypeface(typeface,Typeface.BOLD)
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(246,255,253,244),Color.argb(246,255,242,218))).apply{
    cornerRadius=34f;setStroke(2,Color.rgb(241,199,128))
   }
   elevation=8f
   setPadding(18,17,18,17);setOnClickListener{a()}
  },LinearLayout.LayoutParams(-1,-2).apply{setMargins(9,8,9,8)})
 }
 private fun hiddenNextBtn(s:String,a:()->Unit){
  speechNextButton=Button(this).apply{
   text=s;textSize=20f;setAllCaps(false);setTypeface(typeface,Typeface.BOLD)
   isEnabled=false;alpha=.82f;setTextColor(Color.WHITE)
   background=GradientDrawable().apply{setColor(Color.rgb(174,181,190));cornerRadius=32f;setStroke(2,Color.rgb(151,158,168))}
   setPadding(18,16,18,16);visibility=View.VISIBLE
   setOnClickListener{if(isEnabled)a()}
  }
  root.addView(speechNextButton,LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})
 }
 private fun unlockNextButton(){
  speechNextButton?.apply{
   isEnabled=true;alpha=1f
   background=GradientDrawable().apply{setColor(Color.rgb(74,170,103));cornerRadius=32f;setStroke(2,Color.rgb(54,145,82))}
  }
 }
 private fun say(s:String)=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"cz")
 private fun playWordAndResume(word:String){
  if(!speechScreenActive)return
  cancelListening()
  resumeListeningAfterTts=word
  speechStatus?.text=" Poslouchej slovo…"
  tts.speak(word,TextToSpeech.QUEUE_FLUSH,null,"word_playback")
 }
 private fun retryListeningSoon(delayMs:Long=700L){
  val word=expectedSpeechWord?:return
  if(!speechScreenActive||speechWordPassed)return
  speechHandler.postDelayed({
   if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
  },delayMs)
 }
 private fun stickerCount(key:String)=prefs.getInt("sticker_"+key,0)
 private fun awardSuccess(world:String?=null):RewardGrant{
  val praise=praisePhrases.random()
  val reward=stickerRewards.random()
  val oldCount=stickerCount(reward.key)
  val isNew=oldCount==0
  prefs.edit().putInt("sticker_"+reward.key,oldCount+1).apply()
  stars+=1
  if(world!=null)addWorldProgress(world)
  val allCollected=stickerRewards.all{stickerCount(it.key)>0}
  val bonus=allCollected&&!prefs.getBoolean("sticker_collection_bonus",false)
  if(bonus){
   prefs.edit().putBoolean("sticker_collection_bonus",true).apply()
   stars+=10
  }
  val spokenReward=if(isNew)"Získáváš novou samolepku "+reward.name+"." else "Získáváš samolepku "+reward.name+"."
  val bonusText=if(bonus)" Máš celou sbírku! Dostáváš bonus deset hvězdiček." else ""
  say(praise+" "+spokenReward+bonusText)
  val toast=(if(isNew)" " else " ")+reward.icon+" "+reward.name+" +1"+if(bonus)" BONUS +10 " else ""
  Toast.makeText(this,toast,Toast.LENGTH_LONG).show()
  return RewardGrant(praise,reward,isNew,bonus)
 }
 private fun worldKey(world:String)=normalizeSpeech(world).replace(" ","_")
 private fun worldProgress(world:String)=prefs.getInt("world_progress_"+worldKey(world),0)
 private fun worldStars(world:String):Int=(1+worldProgress(world)/4).coerceIn(1,5)
 private fun addWorldProgress(world:String){
  val before=worldStars(world)
  val key="world_progress_"+worldKey(world)
  prefs.edit().putInt(key,worldProgress(world)+1).apply()
  val after=worldStars(world)
  if(after>before){
   pendingChests+=after-before
   prefs.edit().putString("last_chest_world",world).apply()
  }
 }
 private fun starsRow(world:String):String{
  val n=worldStars(world)
  return "".repeat(n)+"☆".repeat(5-n)
 }
 private fun logoPlaque(subtitle:String="Beriho dobrodružná mapa"){
  val plaque=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
   setPadding(24,18,24,16)
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(234,171,88),Color.rgb(190,109,46))).apply{
    cornerRadius=42f;setStroke(4,Color.argb(235,255,247,220))
   }
   elevation=13f
  }
  plaque.addView(TextView(this).apply{
   text="BERIALO";textSize=38f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)
   setTextColor(Color.rgb(20,92,197));setShadowLayer(4f,0f,2f,Color.WHITE)
  })
  plaque.addView(TextView(this).apply{
   text=subtitle;textSize=17f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)
   setTextColor(Color.rgb(104,51,28))
  })
  root.addView(plaque,LinearLayout.LayoutParams(-1,-2).apply{setMargins(30,4,30,14)})
 }
 private fun statsPanel(){
  val row=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
   setPadding(15,9,15,9)
   background=GradientDrawable().apply{setColor(Color.argb(238,255,250,234));cornerRadius=34f;setStroke(2,Color.rgb(241,199,128))}
   elevation=7f
  }
  row.addView(TwinkleStarsView(this,1,1),LinearLayout.LayoutParams(54,54))
  row.addView(TextView(this).apply{text=stars.toString();textSize=24f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(91,44,24));gravity=Gravity.CENTER})
  row.addView(TextView(this).apply{text=" "+pendingChests;textSize=21f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(91,44,24));gravity=Gravity.CENTER})
  root.addView(row,LinearLayout.LayoutParams(-2,-2).apply{setMargins(0,0,0,10)})
 }
 private fun navBar(){
  val bar=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
   setPadding(8,10,8,10)
   background=GradientDrawable().apply{setColor(Color.argb(246,255,250,239));cornerRadius=38f;setStroke(2,Color.rgb(238,202,139))}
   elevation=12f
  }
  fun add(icon:String,label:String,color:Int,click:()->Unit){
   val item=LinearLayout(this).apply{
    orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(4,7,4,7);setOnClickListener{click()}
   }
   item.addView(TextView(this).apply{
    text=icon;textSize=28f;gravity=Gravity.CENTER
    background=GradientDrawable().apply{setColor(color);shape=GradientDrawable.OVAL;setStroke(2,Color.WHITE)}
    setPadding(10,7,10,7);elevation=7f
   },LinearLayout.LayoutParams(58,58))
   item.addView(TextView(this).apply{text=label;textSize=12f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(84,43,25))})
   bar.addView(item,LinearLayout.LayoutParams(0,-2,1f))
  }
  add("","Učení",Color.rgb(255,191,45)){worlds(false)}
  add("","Výslovnost",Color.rgb(139,91,218)){listen()}
  add("","Hra",Color.rgb(80,190,80)){worlds(true)}
  add("","Odměny",Color.rgb(234,98,74)){rewards()}
  add("","Profil",Color.rgb(73,161,231)){profile()}
  root.addView(bar,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,18,0,4)})
 }
 private fun currentWorldIndex():Int{
  val i=worldList.indexOfFirst{worldStars(it.first)<5}
  return if(i<0)worldList.lastIndex else i
 }
 private fun worldMapCard(world:String,icon:String,index:Int,active:Boolean){
  val holder=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  if(active){
   holder.addView(TextView(this).apply{
    text="BERI JE TADY";textSize=13f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)
    setTextColor(Color.WHITE)
    background=GradientDrawable().apply{setColor(Color.rgb(247,151,35));cornerRadius=22f;setStroke(2,Color.WHITE)}
    setPadding(13,5,13,5)
    animate().alpha(.62f).setDuration(650).withEndAction{animate().alpha(1f).setDuration(650).start()}.start()
   },LinearLayout.LayoutParams(-2,-2).apply{gravity=if(index%2==0)Gravity.START else Gravity.END;setMargins(20,2,20,2)})
  }
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
   setPadding(14,12,14,12)
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.argb(250,255,253,245),Color.argb(250,255,239,210))).apply{
    cornerRadius=38f;setStroke(if(active)4 else 2,if(active)Color.rgb(255,187,40) else Color.rgb(238,205,149))
   }
   elevation=if(active)14f else 8f
   setOnClickListener{worldHub(world)}
  }
  card.addView(TextView(this).apply{
   text=icon;textSize=35f;gravity=Gravity.CENTER
   background=GradientDrawable().apply{setColor(if(active)Color.rgb(255,223,96) else Color.rgb(238,248,255));shape=GradientDrawable.OVAL;setStroke(3,Color.WHITE)}
   setPadding(8,6,8,6);elevation=6f
  },LinearLayout.LayoutParams(64,64))
  val middle=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,0,6,0)}
  middle.addView(TextView(this).apply{
   text=world;textSize=20f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(83,43,24))
  })
  middle.addView(TwinkleStarsView(this,worldStars(world)),LinearLayout.LayoutParams(-1,48))
  middle.addView(TextView(this).apply{
   text=if(worldStars(world)==5)"Dokončeno" else "Úroveň "+worldStars(world)+" / 5"
   textSize=12f;setTextColor(Color.rgb(120,84,61))
  })
  card.addView(middle,LinearLayout.LayoutParams(0,-2,1f))
  card.addView(TextView(this).apply{text="›";textSize=38f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(141,83,48));gravity=Gravity.CENTER},LinearLayout.LayoutParams(38,-1))
  holder.addView(card,LinearLayout.LayoutParams(-1,-2))
  val outer=LinearLayout(this).apply{gravity=if(index%2==0)Gravity.START else Gravity.END}
  outer.addView(holder,LinearLayout.LayoutParams(0,-2,.90f))
  root.addView(outer,LinearLayout.LayoutParams(-1,-2).apply{setMargins(if(index%2==0)2 else 32,7,if(index%2==0)32 else 2,7)})
 } private fun worldHub(world:String){
  base()
  logoPlaque("Svět "+world)
  val head=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(18,16,18,16)
   background=GradientDrawable().apply{setColor(Color.argb(242,255,250,235));cornerRadius=38f;setStroke(3,Color.rgb(244,193,96))}
   elevation=10f
  }
  head.addView(TextView(this).apply{
   text=world;textSize=29f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(82,43,25))
  })
  head.addView(TwinkleStarsView(this,worldStars(world)),LinearLayout.LayoutParams(-1,54))
  root.addView(head,LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,16)})
  bigBtn("UČENÍ","Obrázky • slova • poslech",Color.rgb(255,178,43)){learn(world,0)}
  bigBtn("VÝSLOVNOST","Řekni slovo správně",Color.rgb(139,91,218)){listenWorld(world)}
  bigBtn("HRA","Najdi správnou odpověď",Color.rgb(73,184,78)){quiz(world)}
  btn("Zpět na mapu"){home()}
 } private val domecekImages=mapOf(
 "dům" to R.drawable.word_dum,
 "panelák" to R.drawable.word_panelak,
 "pokoj" to R.drawable.word_pokoj,
 "postel" to R.drawable.word_postel,
 "polštář" to R.drawable.word_polstar,
 "deka" to R.drawable.word_deka,
 "stůl" to R.drawable.word_stul,
 "židle" to R.drawable.word_zidle,
 "okno" to R.drawable.word_okno,
 "dveře" to R.drawable.word_dvere
 )
 private fun drawableFor(x:Word):Int?=if(x.world=="Domeček")domecekImages[x.name]else null
 private fun showWordVisual(x:Word){
  val resId=drawableFor(x)
  if(resId!=null){
   val wrap=FrameLayout(this)
   val picture=ImageView(this).apply{
    setImageResource(resId);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=x.name;adjustViewBounds=true
    background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=34f;setStroke(2,Color.rgb(218,226,235))}
    setPadding(8,8,8,8)
   }
   wrap.addView(picture,FrameLayout.LayoutParams(-1,-1))
   val speaker=TextView(this).apply{
    text="🔊";textSize=24f;gravity=Gravity.CENTER;contentDescription="Přehrát slovo"
    background=GradientDrawable().apply{setColor(Color.rgb(250,252,255));shape=GradientDrawable.OVAL;setStroke(2,Color.rgb(188,199,211))}
    elevation=7f;isClickable=true;isFocusable=true
    setOnClickListener{playWordAndResume(x.name)}
   }
   wrap.addView(speaker,FrameLayout.LayoutParams(72,72,Gravity.END or Gravity.BOTTOM).apply{setMargins(0,0,8,8)})
   root.addView(wrap,LinearLayout.LayoutParams(520,520).apply{setMargins(8,12,8,12)})
  }else if(x.world!="Domeček"){
   val badge=TextView(this).apply{
    text=x.name.take(1).uppercase(Locale("cs","CZ"))
    textSize=58f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)
    setTextColor(Color.WHITE)
    background=GradientDrawable().apply{setColor(Color.rgb(74,151,217));shape=GradientDrawable.OVAL;setStroke(4,Color.WHITE)}
    elevation=8f
   }
   root.addView(badge,LinearLayout.LayoutParams(150,150).apply{setMargins(8,18,8,18)})
  }
 }
 private fun wordBtn(x:Word,a:()->Unit){
  val resId=drawableFor(x)
  if(resId==null){btn(if(x.world=="Domeček") x.name else x.icon+" "+x.name,a);return}
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
   background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(2,Color.rgb(190,218,244))}
   setPadding(12,12,12,14);setOnClickListener{a()}
  }
  card.addView(ImageView(this).apply{setImageResource(resId);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=x.name},
   LinearLayout.LayoutParams(-1,260))
  card.addView(TextView(this).apply{text=x.name;textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.rgb(25,55,100));setTypeface(typeface,Typeface.BOLD)})
  root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})
 }
 private fun splash(){
  base();root.gravity=Gravity.CENTER
  val b=TextView(this).apply{text="";textSize=112f;gravity=Gravity.CENTER;alpha=0f;scaleX=.4f;scaleY=.4f}
  root.addView(b)
  text("BERIALO",36,true)
  text("Uč se s Berim",19)
  b.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).setInterpolator(OvershootInterpolator())
  Handler(Looper.getMainLooper()).postDelayed({home()},1400)
 }
 private fun mapPlace(frame:FrameLayout,v:View,x:Float,y:Float,wf:Float,hf:Float){
  val sw=resources.displayMetrics.widthPixels
  val sh=resources.displayMetrics.heightPixels
  frame.addView(v,FrameLayout.LayoutParams((sw*wf).toInt(),(sh*hf).toInt()).apply{
   leftMargin=(sw*x).toInt()
   topMargin=(sh*y).toInt()
  })
 }
 private fun mapHit(frame:FrameLayout,label:String,x:Float,y:Float,w:Float,h:Float,click:()->Unit){
  val hit=View(this).apply{
   contentDescription=label
   isClickable=true
   isFocusable=true
   background=ColorDrawable(Color.TRANSPARENT)
   setOnClickListener{click()}
  }
  mapPlace(frame,hit,x,y,w,h)
 }
 private fun mapStars(frame:FrameLayout,world:String,x:Float,y:Float,w:Float,h:Float){
  val panel=FrameLayout(this).apply{
   background=GradientDrawable().apply{
    setColor(Color.rgb(255,248,230))
    cornerRadius=22f
   }
   elevation=2f
   isClickable=false
   importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  panel.addView(TwinkleStarsView(this,worldStars(world),5),FrameLayout.LayoutParams(-1,-1))
  mapPlace(frame,panel,x,y,w,h)
 }
 private fun home(){
  speechScreenActive=false
  resumeListeningAfterTts=null
  cancelListening()
  immersiveUi()

  val page=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER_HORIZONTAL
   setPadding(18,24,18,30)
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(213,240,255),Color.rgb(246,250,230)))
  }
  val scroll=ScrollView(this).apply{
   isFillViewport=true
   clipToPadding=false
   addView(page,ViewGroup.LayoutParams(-1,-2))
  }

  val title=TextView(this).apply{
   text="BERIALO";textSize=38f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD)
   setTextColor(Color.rgb(20,92,197));setShadowLayer(4f,0f,2f,Color.WHITE)
   background=GradientDrawable().apply{setColor(Color.rgb(242,190,111));cornerRadius=38f;setStroke(3,Color.WHITE)}
   setPadding(30,14,30,14);elevation=10f
  }
  page.addView(title,LinearLayout.LayoutParams(-1,-2).apply{setMargins(32,4,32,10)})

  val stats=TextView(this).apply{
   text="★  "+stars+"     Poklady  "+pendingChests
   textSize=18f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(88,55,32))
   background=GradientDrawable().apply{setColor(Color.argb(245,255,252,239));cornerRadius=28f;setStroke(2,Color.rgb(235,204,147))}
   setPadding(18,12,18,12)
  }
  page.addView(stats,LinearLayout.LayoutParams(-2,-2).apply{setMargins(0,0,0,14)})

  val map=FrameLayout(this).apply{
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(202,238,255),Color.rgb(226,245,203),Color.rgb(183,224,165))).apply{
    cornerRadius=42f;setStroke(3,Color.rgb(255,255,255))
   }
   setPadding(14,26,14,26)
  }
  val mapColumn=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  map.addView(mapColumn,FrameLayout.LayoutParams(-1,-2))

  val worlds=listOf("Domeček","Zvířata","Jídlo","Barvy a tvary","Škola")
  worlds.forEachIndexed{index,world->
   val side=LinearLayout(this).apply{gravity=if(index%2==0)Gravity.START else Gravity.END}
   val card=LinearLayout(this).apply{
    orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
    setPadding(14,12,14,12);isClickable=true;isFocusable=true;contentDescription=world
    background=GradientDrawable().apply{setColor(Color.rgb(255,252,242));cornerRadius=36f;setStroke(3,Color.rgb(238,199,126))}
    elevation=10f
    setOnClickListener{if(world=="Škola")schoolPrep()else worldHub(world)}
   }
   val icon=TextView(this).apply{
    text=when(world){"Domeček"->"⌂";"Zvířata"->"●";"Jídlo"->"●";"Barvy a tvary"->"◆";else->"A"}
    textSize=30f;gravity=Gravity.CENTER;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(57,113,171))
    background=GradientDrawable().apply{setColor(Color.rgb(238,247,255));shape=GradientDrawable.OVAL;setStroke(2,Color.rgb(196,215,231))}
   }
   card.addView(icon,LinearLayout.LayoutParams(62,62))
   val mid=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,0,8,0)}
   mid.addView(TextView(this).apply{text=world;textSize=19f;setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(76,48,31))})
   mid.addView(TwinkleStarsView(this,worldStars(world),5),LinearLayout.LayoutParams(-1,46))
   card.addView(mid,LinearLayout.LayoutParams(0,-2,1f))
   card.addView(TextView(this).apply{text="›";textSize=34f;gravity=Gravity.CENTER;setTextColor(Color.rgb(125,88,55))},LinearLayout.LayoutParams(38,-1))
   side.addView(card,LinearLayout.LayoutParams((resources.displayMetrics.widthPixels*.72f).toInt(),-2))
   mapColumn.addView(side,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,12,0,12)})
   if(index<worlds.lastIndex){
    mapColumn.addView(TextView(this).apply{text="•\n•\n•";textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.rgb(119,168,103))},
     LinearLayout.LayoutParams(-1,82))
   }
  }
  page.addView(map,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,16)})

  val nav=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
   background=GradientDrawable().apply{setColor(Color.rgb(255,251,239));cornerRadius=32f;setStroke(2,Color.rgb(235,204,147))}
   setPadding(6,8,6,8)
  }
  fun navButton(label:String,click:()->Unit){
   nav.addView(Button(this).apply{
    text=label;textSize=12f;setAllCaps(false);setTypeface(typeface,Typeface.BOLD);setTextColor(Color.rgb(74,55,40))
    background=GradientDrawable().apply{setColor(Color.rgb(250,247,238));cornerRadius=22f;setStroke(1,Color.rgb(220,211,195))}
    isClickable=true;isFocusable=true;setOnClickListener{click()}
   },LinearLayout.LayoutParams(0,72,1f).apply{setMargins(3,0,3,0)})
  }
  navButton("Učení"){worlds(false)}
  navButton("Výslovnost"){listen()}
  navButton("Hra"){worlds(true)}
  navButton("Odměny"){rewards()}
  navButton("Profil"){profile()}
  page.addView(nav,LinearLayout.LayoutParams(-1,-2))

  setContentView(scroll)
 }
 private fun worlds(q:Boolean){
  base();text("Vyber si svět",30,true)
  worldList.forEach{w->btn(w.first+"   •   Úroveň "+worldStars(w.first)+"/5"){if(q)quiz(w.first)else learn(w.first,0)}}
  btn("Mapa"){home()}
 }
 private fun learn(world:String,i:Int){
  speechWordPassed=false;speechNextButton=null
  val ws=words.filter{it.world==world};val x=ws[i%ws.size]
  base();speechScreenActive=true;expectedSpeechWord=x.name;expectedSpeechWorld=world
  text(world,25,true);showWordVisual(x);text(x.name.uppercase(),34,true)
  btn("Přehrát slovo"){playWordAndResume(x.name)}
  speechInfo()
  hiddenNextBtn("Další  ➜"){learn(world,i+1)}
  btn("Procvičit"){quiz(world)}
  btn("Mapa"){home()}
  speechHandler.postDelayed({if(speechScreenActive&&!speechWordPassed)startListening(x.name)},250)
 }
 private fun quiz(world:String){val pool=words.filter{it.world==world};val target=pool.random();val choices=(pool.filter{it!=target}.shuffled().take(3)+target).shuffled();base();text("Najdi správný obrázek",25,true);text("Najdi: "+target.name,30,true);btn("Přehrát zadání"){say("Najdi "+target.name)};choices.forEach{c->wordBtn(c){if(c==target){awardSuccess(world);quiz(world)}else{say("Zkus to ještě jednou");Toast.makeText(this,"Zkus to ještě jednou",Toast.LENGTH_SHORT).show()}}};btn("⌂  Domů"){home()}}
 private fun listen(){listenWorld(null)}
 private fun listenWorld(world:String?){
  speechWordPassed=false;speechNextButton=null
  val pool=if(world==null)words else words.filter{it.world==world}
  val x=pool.random()
  base();speechScreenActive=true;expectedSpeechWord=x.name;expectedSpeechWorld=x.world
  text("Výslovnost",27,true);text(x.world,18,true);showWordVisual(x);text(x.name,34,true)
  btn("Přehrát slovo"){playWordAndResume(x.name)}
  speechInfo(" Mikrofon poslouchá. Řekni zobrazené slovo.")
  hiddenNextBtn("Další slovo  ➜"){listenWorld(world)}
  btn("Mapa"){home()}
  speechHandler.postDelayed({if(speechScreenActive&&!speechWordPassed)startListening(x.name)},250)
 }
 private fun startListening(word:String){
  expectedSpeechWord=word
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_RECORD_AUDIO)
   return
  }
  beginSpeechRecognition()
 }
 private fun beginSpeechRecognition(){
  val target=expectedSpeechWord?:return
  if(!speechScreenActive||speechWordPassed)return
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   speechStatus?.text="Rozpoznávání řeči není v tomto telefonu dostupné."
   Toast.makeText(this,"Rozpoznávání řeči není dostupné",Toast.LENGTH_LONG).show()
   return
  }
  if(speechRecognizer==null){
   speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this).also{r->
    r.setRecognitionListener(object:RecognitionListener{
     override fun onReadyForSpeech(params:Bundle?){speechStatus?.text=" Poslouchám… řekni slovo."}
     override fun onBeginningOfSpeech(){speechStatus?.text=" Slyším tě…"}
     override fun onRmsChanged(rmsdB:Float){}
     override fun onBufferReceived(buffer:ByteArray?){}
     override fun onEndOfSpeech(){speechStatus?.text="Kontroluji slovo…"}
     override fun onError(error:Int){
      speechStatus?.text=when(error){
       SpeechRecognizer.ERROR_NO_MATCH->"Nerozuměl jsem. Poslouchám znovu…"
       SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"Nic jsem neslyšel. Poslouchám znovu…"
       SpeechRecognizer.ERROR_AUDIO->"Mikrofon měl problém. Zkouším znovu…"
       SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"Aplikace nemá povolený mikrofon."
       SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"Rozpoznávání řeči teď není dostupné. Zkouším znovu…"
       else->"Nepodařilo se rozpoznat slovo. Poslouchám znovu…"
      }
      if(error!=SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)retryListeningSoon(900)
     }
     override fun onResults(results:Bundle?){
      val heard=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
      checkSpokenWord(heard)
     }
     override fun onPartialResults(partialResults:Bundle?){}
     override fun onEvent(eventType:Int,params:Bundle?){}
    })
   }
  }
  speechStatus?.text=" Připravuji mikrofon…"
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE,"cs-CZ")
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
   putExtra(RecognizerIntent.EXTRA_PROMPT,target)
  }
  try{
   speechRecognizer?.cancel()
   speechRecognizer?.startListening(intent)
  }catch(_:Exception){
   speechStatus?.text="Mikrofon se nepodařilo spustit. Zkus to znovu."
  }
 }
 private fun normalizeSpeech(s:String):String{
  val plain=Normalizer.normalize(s.lowercase(Locale("cs","CZ")),Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"")
  return plain.replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 }
 private fun checkSpokenWord(results:List<String>){
  val target=expectedSpeechWord?:return
  val wanted=normalizeSpeech(target)
  val pattern=Regex("(^| )"+Regex.escape(wanted)+"($| )")
  val ok=results.any{candidate->pattern.containsMatchIn(normalizeSpeech(candidate))}
  if(ok){
   if(!speechWordPassed){
    speechWordPassed=true
    val grant=awardSuccess(expectedSpeechWorld)
    speechStatus?.text=" "+grant.praise+" "+grant.reward.icon+" "+grant.reward.name+" •  Teď můžeš pokračovat dál."
   }else{
    speechStatus?.text=" Tohle slovo už máš splněné. Můžeš pokračovat dál."
   }
   unlockNextButton()
  }else{
   val best=results.firstOrNull()?.trim().orEmpty()
   speechStatus?.text=if(best.isBlank())"Nerozuměl jsem. Poslouchám znovu…" else "Slyšel jsem „"+best+"“. Poslouchám znovu, řekni: "+target+"."
   retryListeningSoon(700)
  }
 }
 private fun cancelListening(){try{speechRecognizer?.cancel()}catch(_:Exception){}}
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==REQ_RECORD_AUDIO){
   if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED)beginSpeechRecognition()
   else speechStatus?.text="Bez povolení mikrofonu nemůžu poslouchat výslovnost."
  }
 }
 private fun schoolPrep(){base();text("Příprava do školy",30,true);text("Vyber si hru",22,true);bigBtn("KDE TO JE?","na • pod • vedle • před • za",Color.rgb(91,155,213)){positionGame()};bigBtn("POČÍTÁNÍ","od 1 do 20",Color.rgb(245,166,35)){countGame()};bigBtn("PROTIKLADY","velký × malý",Color.rgb(139,101,207)){oppositesGame()};bigBtn("ROZUMÍM","pokyny a situace",Color.rgb(70,170,120)){instructionGame()};btn("Domů"){home()}}
 private fun positionGame(){val tasks=listOf("Míč je NA stole." to "na","Kočka je POD stolem." to "pod","Medvídek je VEDLE židle." to "vedle","Auto je PŘED domem." to "před","Pes je ZA domem." to "za","Kostka je V krabici." to "v","Židle je MEZI stoly." to "mezi");val t=tasks.random();base();text("Kde to je?",28,true);text(t.first,27,true);btn("Poslechni"){say(t.first)};listOf("na","pod","vedle","před","za","v","mezi").shuffled().take(4).let{xs->val opts=(xs+t.second).distinct().shuffled().take(4);opts.forEach{o->btn(o.uppercase()){if(o==t.second){awardSuccess("Škola");positionGame()}else say("Zkus to znovu")}}};btn("⌂ Domů"){home()}}
 private fun countGame(){val n=(1..20).random();val options=listOf(n,(n+1).coerceAtMost(20),(n-1).coerceAtLeast(1),(1..20).random()).distinct().shuffled();base();text("Počítání do 20",28,true);text("".repeat(n),22);text("Kolik je hvězdiček?",22,true);options.forEach{o->btn(o.toString()){if(o==n){awardSuccess("Škola");countGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun oppositesGame(){val pairs=listOf("velký" to "malý","rychlý" to "pomalý","teplý" to "studený","nahoře" to "dole","den" to "noc","plný" to "prázdný","otevřený" to "zavřený","veselý" to "smutný","dlouhý" to "krátký","čistý" to "špinavý");val p=pairs.random();val choices=(pairs.flatMap{listOf(it.first,it.second)}.filter{it!=p.first}.shuffled().take(3)+p.second).shuffled();base();text("Najdi protiklad",28,true);text(p.first.uppercase(),32,true);choices.forEach{o->btn(o){if(o==p.second){awardSuccess("Škola");oppositesGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun instructionGame(){val tasks=listOf("Co uděláš, když učitel řekne: Otevři knihu?" to "otevřu knihu","Co uděláš před přechodem přes silnici?" to "rozhlédnu se","Co řekneš, když o něco žádáš?" to "prosím","Co řekneš, když ti někdo pomůže?" to "děkuji","Kterou rukou ukazuješ doprava?" to "pravou");val t=tasks.random();val wrong=listOf("zavřu oči","uteču","nevím","nic neřeknu","otočím se");val choices=(wrong.shuffled().take(3)+t.second).shuffled();base();text("Rozumím pokynům",28,true);text(t.first,22,true);btn("Poslechni"){say(t.first)};choices.forEach{o->btn(o){if(o==t.second){awardSuccess("Škola");instructionGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun treasureHunt(){
  base()
  val place=prefs.getString("last_chest_world","na cestě")?:"na cestě"
  text("Beri našel poklad!",30,true)
  text("",82)
  text("Truhla čeká ve světě: "+place,19,true)
  text("",110)
  if(pendingChests>0){
   bigBtn("OTEVŘÍT TRUHLU","Zjisti, co Beri našel",Color.rgb(244,174,48)){openTreasure()}
  }else text("Teď žádná truhla nečeká. Získej další hvězdu.",18)
  btn("Zpět na mapu"){home()}
 }
 private fun openTreasure(){
  if(pendingChests<=0){home();return}
  val reward=treasurePool().random()
  val old=treasureCount(reward.key)
  prefs.edit().putInt("treasure_"+reward.key,old+1).apply()
  pendingChests-=1
  base()
  text(if(old==0)" NOVÝ POKLAD!" else " DALŠÍ POKLAD!",30,true)
  text(reward.icon,116)
  text(reward.name,30,true)
  text("Beri ho přidal do tvé sbírky.",19)
  say("Beri našel "+reward.name+". Skvělá práce!")
  if(pendingChests>0)bigBtn("OTEVŘÍT DALŠÍ","Ještě jedna truhla čeká",Color.rgb(244,174,48)){treasureHunt()}
  btn("Moje sbírka"){rewards()}
  btn("Mapa"){home()}
 }
 private fun profile(){
  base()
  text("Profil dítěte",30,true)
  text("Kdo si dnes hraje s Berim?",18)
  val p=childProfile()
  btn((if(p=="boy")" " else "")+" Kluk"){prefs.edit().putString("child_profile","boy").apply();profile()}
  btn((if(p=="girl")" " else "")+" Holka"){prefs.edit().putString("child_profile","girl").apply();profile()}
  btn((if(p=="neutral")" " else "")+" Nechci řešit"){prefs.edit().putString("child_profile","neutral").apply();profile()}
  text("Co má dítě nejraději?",22,true)
  text("Tohle má větší vliv na poklady než samotná volba kluk/holka.",15)
  val theme=rewardTheme()
  btn((if(theme=="adventure")" " else "")+" Dobrodružství"){prefs.edit().putString("reward_theme","adventure").apply();profile()}
  btn((if(theme=="magic")" " else "")+" Kouzelný svět"){prefs.edit().putString("reward_theme","magic").apply();profile()}
  btn((if(theme=="animals")" " else "")+" Zvířata"){prefs.edit().putString("reward_theme","animals").apply();profile()}
  btn((if(theme=="creative")" " else "")+" Tvoření a hry"){prefs.edit().putString("reward_theme","creative").apply();profile()}
  btn("Nechat Beriho vybrat"){prefs.edit().remove("reward_theme").apply();profile()}
  btn("Zpět na mapu"){home()}
 }
 private fun rewards(){
  base()
  text("Beriho sbírka",30,true)
  text(""+stars,42,true)
  val collected=stickerRewards.count{stickerCount(it.key)>0}
  text("Samolepky "+collected+" / "+stickerRewards.size,22,true)
  stickerRewards.forEach{r->
   val count=stickerCount(r.key)
   text(if(count>0)r.icon+" "+r.name+" × "+count else " "+r.name,19)
  }
  text("Poklady z mapy",24,true)
  val pool=(adventureTreasures+magicTreasures+animalTreasures+creativeTreasures).distinctBy{it.key}
  val found=pool.count{treasureCount(it.key)>0}
  text("Nalezeno "+found+" různých pokladů • celkem "+vocabularyTreasureCount(),17)
  pool.filter{treasureCount(it.key)>0}.forEach{r->text(r.icon+" "+r.name+" × "+treasureCount(r.key),19)}
  if(found==0)text("Zatím žádný poklad. Nová hvězda ve světě odemkne truhlu.",17)
  if(pendingChests>0)bigBtn("OTEVŘÍT TRUHLU","Čeká jich: "+pendingChests,Color.rgb(244,174,48)){treasureHunt()}
  btn("Upravit profil a zájmy"){profile()}
  btn("Zpět na mapu"){home()}
 }
 override fun onBackPressed(){home()}
 override fun onWindowFocusChanged(hasFocus:Boolean){
  super.onWindowFocusChanged(hasFocus)
  if(hasFocus)immersiveUi()
 }
 override fun onDestroy(){try{speechRecognizer?.cancel();speechRecognizer?.destroy();speechRecognizer=null}catch(_:Exception){};tts.stop();tts.shutdown();super.onDestroy()}
}