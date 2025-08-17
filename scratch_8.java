// ===================== scratch_8.java (ONLINE 1v1, ALFABETİK SIRALI TAM DOSYA) =====================
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class scratch_8 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("ComplexClash — Complex Number Card Duel (Online 1v1)");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setResizable(false);
            GamePanel gp = new GamePanel();
            f.setContentPane(gp);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            gp.start();
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { gp.stop(); }
            });
        });
    }

    /* =========================  ALFABETİK: Tür Tanımları  ========================= */

    // A
    enum Ability { MANA_BOOST, SWAP_VALUES, BAN_ADD }

    static class Area {
        Complex value = Complex.ZERO;
        Card pending = null;
        void setValue(Complex z){ value=z; }
        void clear(){ value=Complex.ZERO; pending=null; }
    }

    // B
    interface BinaryOp { Complex apply(Complex x, Complex y); }

    static class Button {
        Rectangle r; String text;
        Button(int x,int y,int w,int h,String t){ r=new Rectangle(x,y,w,h); text=t; }
        boolean contains(Point p){ return r.contains(p); }
        void draw(Graphics2D g){
            g.setColor(new Color(0,0,0,120)); g.fillRoundRect(r.x+4,r.y+4,r.width,r.height,12,12);
            g.setColor(new Color(80,160,120)); g.fillRoundRect(r.x,r.y,r.width,r.height,12,12);
            g.setColor(Color.BLACK); g.setStroke(new BasicStroke(2f)); g.drawRoundRect(r.x,r.y,r.width,r.height,12,12);
            g.setColor(Color.WHITE); Font f=g.getFont(); g.setFont(f.deriveFont(Font.BOLD, 18f));
            FontMetrics fm=g.getFontMetrics(); int tx=r.x+(r.width-fm.stringWidth(text))/2; int ty=r.y+(r.height+fm.getAscent()-fm.getDescent())/2;
            g.drawString(text, tx, ty); g.setFont(f);
        }
    }

    // C
    static class Card {
        String name; String desc; int cost; CardType type;
        Complex value; UnaryOp unary; BinaryOp binary; Ability ability; String opName;
        Card(String name,String desc,int cost,CardType type){this.name=name;this.desc=desc;this.cost=cost;this.type=type;}
        Card copy(){ Card c=new Card(name,desc,cost,type); c.value=value==null?null:value.copy(); c.unary=unary; c.binary=binary; c.ability=ability; c.opName=opName; return c; }
    }

    static class CardAnim {
        Card c; Rectangle from, to; int life, max; Runnable onDone;
        CardAnim(Card c, Rectangle from, Rectangle to, int frames, Runnable onDone){
            this.c=c; this.from=new Rectangle(from); this.to=new Rectangle(to); this.life=frames; this.max=frames; this.onDone=onDone;
        }
        boolean step(){ life--; if (life<=0){ if (onDone!=null) onDone.run(); return false; } return true; }
        void draw(Graphics2D g){
            double t = 1.0 - (life/(double)max);
            int x = (int)Math.round(lerp(from.x, to.x, t));
            int y = (int)Math.round(lerp(from.y, to.y, t));
            int w = (int)Math.round(lerp(from.width, to.width, t));
            int h = (int)Math.round(lerp(from.height, to.height, t));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g.setColor(new Color(64,68,90)); g.fillRoundRect(x,y,w,h,10,10);
            g.setColor(new Color(110,120,140)); g.drawRoundRect(x,y,w,h,10,10);
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(Color.WHITE); g.drawString(c.name, x+8, y+22);
        }
        double lerp(double a,double b,double t){ return a+(b-a)*t; }
    }

    static class CardFactory {
        static Card number(String name, Complex v, int cost){ Card c=new Card(name, v.toShort(), cost, CardType.NUMBER); c.value=v; return c; }
        static Card unary(String name, String desc, int cost, UnaryOp f){ Card c=new Card(name,desc,cost,CardType.UNARY); c.unary=f; c.opName=name; return c; }
        static Card binary(String name, String desc, int cost, BinaryOp f){ Card c=new Card(name,desc,cost,CardType.BINARY); c.binary=f; c.opName=name; return c; }
        static Card ability(String name, String desc, int cost, Ability a){ Card c=new Card(name,desc,cost,CardType.ABILITY); c.ability=a; return c; }

        static List<Card> fullPool(Random rng){ List<Card> pool=new ArrayList<>();
            for(int k=-10;k<=10;k++) if (k!=0) pool.add(number(Integer.toString(k), new Complex(k,0), 0));
            pool.add(number("i", Complex.I, 0)); pool.add(number("-i", Complex.I.neg(), 0));
            pool.add(number("π", new Complex(Math.PI,0), 1)); pool.add(number("e", new Complex(Math.E,0), 1));
            pool.add(number("π/2", new Complex(Math.PI/2,0), 1)); pool.add(number("√2", new Complex(Math.sqrt(2),0), 1));

            pool.add(unary("-X", "Negasyon", 1, Complex::neg));
            pool.add(unary("conj(X)", "Eşlenik", 1, Complex::conj));
            pool.add(unary("√X", "Karekök (principal)", 2, Complex::sqrt));
            pool.add(unary("|X|", "Mutlak değer (reel)", 2, x->new Complex(x.abs(),0)));
            pool.add(unary("arg(X)", "Açı (rad)", 1, x->new Complex(x.arg(),0)));
            pool.add(unary("Re(X)", "Gerçek kısım", 1, x->new Complex(x.re,0)));
            pool.add(unary("Im(X)", "İmajiner kısım", 1, x->new Complex(x.im,0)));
            pool.add(unary("Re+Im", "Koordinatlar toplamı (reel)", 1, x->new Complex(x.re+x.im,0)));

            pool.add(binary("X+Y", "Topla", 1, Complex::add));
            pool.add(binary("X-Y", "Çıkar", 1, Complex::sub));
            pool.add(binary("X*Y", "Çarp", 2, Complex::mul));
            pool.add(binary("X/Y", "Böl", 2, Complex::div));
            pool.add(binary("X^Y", "Üs (kompleks)", 3, Complex::pow));
            pool.add(binary("X mod Y", "Reel mod", 1, (x,y)-> new Complex((int)Math.round(x.re)% Math.max(1,(int)Math.round(y.re)),0)));

            pool.add(ability("+3 Mana", "Sonraki tur hedef oyuncuya +3 mana", 1, Ability.MANA_BOOST));
            pool.add(ability("Swap", "İki oyuncunun sayılarını takas et", 2, Ability.SWAP_VALUES));
            pool.add(ability("Ban X+Y", "Hedef oyuncu 3 tur X+Y oynayamaz", 1, Ability.BAN_ADD));

            for(int k=0;k<40;k++){
                int a = rng.nextInt(9)+2;
                pool.add(number("+"+a, new Complex(a,0), 0));
                pool.add(unary("X+"+a, "X + "+a, 1, x->x.add(a)));
            }
            List<Card> copy=new ArrayList<>(); for(Card c:pool){ copy.add(c.copy()); }
            pool.addAll(copy);
            return pool;
        }
    }

    enum CardType { NUMBER, UNARY, BINARY, ABILITY }

    static class Complex {
        final double re, im;
        static final Complex ZERO = new Complex(0,0);
        static final Complex I = new Complex(0,1);
        Complex(double re,double im){ this.re=re; this.im=im; }
        Complex copy(){ return new Complex(re,im); }
        Complex add(Complex o){ return new Complex(re+o.re, im+o.im); }
        Complex add(double k){ return new Complex(re+k, im); }
        Complex sub(Complex o){ return new Complex(re-o.re, im-o.im); }
        Complex mul(Complex o){ return new Complex(re*o.re - im*o.im, re*o.im + im*o.re); }
        Complex mul(double k){ return new Complex(re*k, im*k); }
        Complex div(Complex o){ double d=o.re*o.re + o.im*o.im; if (d==0) return new Complex(1e9,1e9); return new Complex((re*o.re+im*o.im)/d, (im*o.re - re*o.im)/d); }
        Complex neg(){ return new Complex(-re,-im); }
        Complex conj(){ return new Complex(re, -im); }
        double abs(){ return Math.hypot(re,im); }
        double arg(){ return Math.atan2(im,re); }
        Complex sqrt(){ double r = abs(); double t = Math.sqrt((r+re)/2.0); double u = Math.signum(im)*Math.sqrt((r-re)/2.0); return new Complex(t,u); }
        Complex log(){ return new Complex(Math.log(abs()), arg()); }
        Complex exp(){ double e=Math.exp(re); return new Complex(e*Math.cos(im), e*Math.sin(im)); }
        Complex pow(Complex w){ return w.mul(this.log()).exp(); }

        static Complex fromPolar(double r, double ang){ return new Complex(r*Math.cos(ang), r*Math.sin(ang)); }
        static Complex fromPolar(int r, double ang){ return fromPolar((double)r, ang); }

        public String toString(){ return String.format(Locale.US, "%.3f%+.3fi", re, im); }
        String toShort(){
            if (Math.abs(im) < 1e-9) return String.format(Locale.US, "%.3f", re);
            if (Math.abs(re) < 1e-9) return String.format(Locale.US, "%.3fi", im);
            return String.format(Locale.US, "%.2f%+.2fi", re, im);
        }
        String toStringNice(){ return toShort(); }
    }

    // F
    static class FloatingText {
        String s; double x,y,vy; int life;
        static FloatingText forArea(Area a, String s){ FloatingText f=new FloatingText(); f.s=s; f.life=60; f.vy=-0.6; f.x=100; f.y=100; return f; }
        static FloatingText forCenter(Rectangle r, String s){ FloatingText f=new FloatingText(); f.s=s; f.x=r.x+r.width/2.0-40; f.y=r.y+24; f.vy=-0.7; f.life=60; return f; }
        boolean step(){ y+=vy; life--; return life>0; }
        void draw(Graphics2D g){ g.setColor(new Color(255,255,255,180)); g.drawString(s,(int)x,(int)y); }
    }

    // G
    static class GamePanel extends JPanel implements MouseListener, KeyListener {
        static final int WIDTH = 1280, HEIGHT = 720;

        private Random rng = new Random();
        private Timer timer;
        private static boolean ERROR_PRINTED = false;

        Screen screen = Screen.MENU;
        double transition = 1.0;
        boolean transitioning = false;
        Screen nextScreen = Screen.MENU;

        Settings settings = new Settings();

        Player me = new Player("SEN");
        Player ai = new Player("OPP");

        Complex target; Complex bomb;

        final Deque<Card> drawPile = new ArrayDeque<>();
        final List<Card> discard = new ArrayList<>();
        boolean myTurn = true; int consecutivePasses = 0; boolean gameOver=false; String endText="";
        int aiStepsThisTurn = 0; int aiMaxSteps(){ return settings.aiSteps; }
        int aiThinkCooldown = 0;

        boolean online = false;
        NetRole netRole = NetRole.NONE;
        NetPeer peer = null;
        boolean waitingPeer = false;
        long syncSeed = 0L;
        boolean hostStarts = true;

        Rectangle rMyField, rAiField, rMyCraft, rAiCraft, rHandArea, rDraw, rDiscard, rPlane, rEndTurn, rClaim, rHelpBtn, rPauseBtn;
        List<Rectangle> rHandSlots = new ArrayList<>();
        String hint = "Bir kart seç, sonra hedef alana tıkla.";
        Card selectedCard = null;

        Button btnStart, btnHost, btnJoin, btnHelp, btnSettings, btnQuit;
        Button btnResume, btnRestart, btnBackToMenu;

        List<CardAnim> anims = new ArrayList<>();
        List<FloatingText> floats = new ArrayList<>();
        LinkedList<String> log = new LinkedList<>(); int LOG_MAX=8;

        public GamePanel(){
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(new Color(18,20,28));
            addMouseListener(this);
            setFocusable(true);
            addKeyListener(this);

            layoutRects();
            buildMenus();
            buildGameRandom();
            timer = new Timer(1000/60, e -> { tick(); repaint(); });
        }

        void start(){ if (!timer.isRunning()) timer.start(); }
        void stop(){ if (timer!=null) timer.stop(); }

        void layoutRects(){
            rMyField = new Rectangle(60, HEIGHT-260, 360, 160);
            rAiField = new Rectangle(60, 80, 360, 160);
            rMyCraft = new Rectangle(450, HEIGHT-220, 240, 120);
            rAiCraft = new Rectangle(450, 120, 240, 120);
            rHandArea= new Rectangle(720, HEIGHT-240, 520, 200);
            rDraw    = new Rectangle(WIDTH/2-45, HEIGHT/2-60, 90, 120);
            rDiscard = new Rectangle(WIDTH/2+60, HEIGHT/2-60, 120, 120);
            rPlane   = new Rectangle(720, 60, 520, 360);
            rEndTurn = new Rectangle(60, HEIGHT-80, 180, 40);
            rClaim   = new Rectangle(450, HEIGHT-90, 180, 40);
            rHelpBtn = new Rectangle(WIDTH-120, 14, 100, 28);
            rPauseBtn= new Rectangle(WIDTH-230, 14, 100, 28);

            rHandSlots.clear();
            for(int i=0;i<5;i++){
                int w=100,h=150; int gap=18; int x=rHandArea.x+20+i*(w+gap); int y=rHandArea.y+22; rHandSlots.add(new Rectangle(x,y,w,h));
            }
        }

        void buildMenus(){
            int cx = centerX(260);
            btnStart    = new Button(cx, 210, 260, 56, "YENİ OYUN (LOCAL)");
            btnHost     = new Button(cx, 280, 260, 56, "HOST (ONLINE)");
            btnJoin     = new Button(cx, 350, 260, 56, "JOIN (ONLINE)");
            btnHelp     = new Button(cx, 420, 260, 56, "NASIL OYNANIR");
            btnSettings = new Button(cx, 490, 260, 56, "AYARLAR");
            btnQuit     = new Button(cx, 560, 260, 56, "ÇIKIŞ");

            btnResume     = new Button(centerX(260), 320, 260, 56, "DEVAM");
            btnRestart    = new Button(centerX(260), 390, 260, 56, "YENİDEN BAŞLAT");
            btnBackToMenu = new Button(centerX(260), 460, 260, 56, "ANA MENÜ");
        }

        void buildGameRandom(){ buildGameWithGivenRng(new Random(), false, true); }
        void buildGameWithSeed(long seed, boolean hostStarts){
            this.syncSeed = seed;
            this.hostStarts = hostStarts;
            buildGameWithGivenRng(new Random(seed), true, hostStarts);
        }

        void buildGameWithGivenRng(Random r, boolean netStartKnown, boolean hostStartsVal){
            this.rng = r;

            List<Complex> pool = NumberPools.bigPool();
            target = pool.get(rng.nextInt(pool.size()));
            bomb   = pool.get(rng.nextInt(pool.size()));
            while (almostEqual(bomb, target)) bomb = pool.get(rng.nextInt(pool.size()));

            drawPile.clear(); discard.clear();
            List<Card> all = CardFactory.fullPool(rng);
            Collections.shuffle(all, rng);
            int drawCount = Math.min(settings.deckCount, all.size());
            for (int i=0;i<drawCount; i++) drawPile.add(all.get(i).copy());

            me = new Player("SEN"); ai = new Player("OPP");
            me.field.value = Complex.ZERO; ai.field.value = Complex.ZERO;
            me.drawUpTo(drawPile,5); ai.drawUpTo(drawPile,5);

            if (online && netStartKnown){
                if (netRole==NetRole.HOST) myTurn = hostStartsVal; else myTurn = !hostStartsVal;
            } else {
                myTurn=true;
            }
            me.newTurn(); ai.newTurn();

            aiStepsThisTurn = 0; aiThinkCooldown = 0;
            selectedCard=null; hint="Bir kart seç, sonra hedef alana tıkla.";
            gameOver=false; endText=""; floats.clear(); anims.clear(); log.clear(); consecutivePasses=0;
            addLog((online?"ONLINE ":"LOCAL ")+"Yeni oyun: hedef="+target+" | bomba="+bomb);
        }

        void nextTurn(){
            if (gameOver) return;
            myTurn = !myTurn;
            if (myTurn) {
                me.newTurn(); me.drawUpTo(drawPile,5);
                hint = "SENİN TURUN: 5 mana. Kart seç → hedef alan";
                addLog("Tur: SEN");
            } else {
                ai.newTurn(); ai.drawUpTo(drawPile,5);
                aiStepsThisTurn = 0; aiThinkCooldown = 30;
                addLog(online? "Tur: RAKİP" : "Tur: AI");
            }
        }

        static class Move { Card card; Area target; Card yCard; double score; int handIndex=-1; int yIndex=-1; }

        Move findBestAIMove(){
            List<Move> options = new ArrayList<>();
            for (int ci=0; ci<ai.hand.size(); ci++){
                Card c = ai.hand.get(ci);
                if (c.cost>ai.mana) continue;
                if (c.type==CardType.NUMBER){
                    Move m = new Move(); m.card=c; m.handIndex=ci; m.target=ai.field;
                    m.score = -dist(c.value, target);
                    options.add(m);
                } else if (c.type==CardType.UNARY){
                    Complex nv = c.unary.apply(ai.field.value);
                    Move m=new Move(); m.card=c; m.handIndex=ci; m.target=ai.field; m.score=-dist(nv,target); options.add(m);
                } else if (c.type==CardType.BINARY){
                    for (int yi=0; yi<ai.hand.size(); yi++){
                        Card y = ai.hand.get(yi);
                        if (y==c || y.type!=CardType.NUMBER) continue;
                        if (c.cost+y.cost>ai.mana) continue;
                        Complex nv = c.binary.apply(ai.field.value, y.value);
                        Move m=new Move(); m.card=c; m.handIndex=ci; m.yCard=y; m.yIndex=yi; m.target=ai.field; m.score=-dist(nv,target); options.add(m);
                    }
                } else if (c.type==CardType.ABILITY){
                    Move m=new Move(); m.card=c; m.handIndex=ci; m.target=ai.field;
                    if (c.ability==Ability.MANA_BOOST) m.score=0.1; else if (c.ability==Ability.SWAP_VALUES) m.score=-dist(me.field.value,target);
                    else m.score=0;
                    options.add(m);
                }
            }
            for (int ci=0; ci<ai.hand.size(); ci++){
                Card c = ai.hand.get(ci);
                if (c.cost>ai.mana) continue;
                if (c.type==CardType.UNARY){
                    Complex nv = c.unary.apply(me.field.value);
                    Move m=new Move(); m.card=c; m.handIndex=ci; m.target=me.field; m.score=dist(nv,target)-dist(me.field.value,target); options.add(m);
                } else if (c.type==CardType.BINARY){
                    for (int yi=0; yi<ai.hand.size(); yi++){
                        Card y= ai.hand.get(yi);
                        if (y==c||y.type!=CardType.NUMBER) continue; if (c.cost+y.cost>ai.mana) continue;
                        Complex nv=c.binary.apply(me.field.value,y.value);
                        Move m=new Move(); m.card=c; m.handIndex=ci; m.yCard=y; m.yIndex=yi; m.target=me.field; m.score=dist(nv,target)-dist(me.field.value,target); options.add(m);
                    }
                }
            }
            Move best=null; for(Move m:options){ if(best==null||m.score>best.score) best=m; }
            return best;
        }

        void aiActTick(){
            if (myTurn || gameOver) return;
            if (online) return;
            if (aiThinkCooldown>0){ aiThinkCooldown--; return; }
            if (aiStepsThisTurn >= aiMaxSteps() || ai.mana<=0){ nextTurn(); return; }

            Move best = findBestAIMove();
            if (best==null){ nextTurn(); return; }
            playMoveAnimated(ai, best, false);
            aiStepsThisTurn++;
            aiThinkCooldown = settings.aiDelayFrames;
        }

        void playMoveAnimated(Player p, Move m, boolean initiatedLocal){
            Rectangle from = slotOf(p, m.card);
            Rectangle to   = areaRect(m.target);
            if (from == null) from = new Rectangle(WIDTH/2 - 40, HEIGHT/2 - 50, 80, 100);
            if (to   == null) to   = new Rectangle(WIDTH/2 - 40, HEIGHT/2 - 50, 80, 100);

            final boolean hasY = (m.card.type == CardType.BINARY && m.yCard != null);
            final Rectangle preFrom2 = hasY ? slotOf(p, m.yCard) : null;
            final Rectangle preTo2   = hasY ? areaRect(m.target) : null;
            final Card yCopy         = hasY ? m.yCard.copy() : null;

            final Card playCopy   = m.card.copy();
            final Rectangle fromF = new Rectangle(from);
            final Rectangle toF   = new Rectangle(to);

            anims.add(new CardAnim(playCopy, fromF, toF, 18, () -> {
                applyMove(p, m);
                if (online && initiatedLocal){
                    netSendMove(m, targetIdOf(m.target));
                }
                if (hasY){
                    Rectangle f2 = (preFrom2 != null) ? new Rectangle(preFrom2) : new Rectangle(fromF);
                    Rectangle t2 = (preTo2   != null) ? new Rectangle(preTo2)   : new Rectangle(toF);
                    anims.add(new CardAnim(yCopy, f2, t2, 16, () -> {}));
                }
            }));
        }

        Rectangle slotOf(Player p, Card c){
            int idx = p.hand.indexOf(c);
            if (idx>=0 && idx<rHandSlots.size()){
                if (p==me) return new Rectangle(rHandSlots.get(idx));
                return new Rectangle(rAiField.x + rAiField.width + 40, rAiField.y, 80, 110);
            }
            return null;
        }
        Rectangle areaRect(Area a){
            if (a==me.field) return new Rectangle(rMyField.x+rMyField.width-80, rMyField.y+20, 80,120);
            if (a==ai.field) return new Rectangle(rAiField.x+rAiField.width-80, rAiField.y+20, 80,120);
            if (a==me.craft) return new Rectangle(rMyCraft.x+rMyCraft.width-80, rMyCraft.y+10, 80,100);
            if (a==ai.craft) return new Rectangle(rAiCraft.x+rAiCraft.width-80, rAiCraft.y+10, 80,100);
            return null;
        }

        void applyMove(Player p, Move m){
            Player tgtOwner = (m.target==me.field||m.target==me.craft)?me:ai;

            if (m.card.type==CardType.BINARY){
                if (p.mana < m.card.cost + (m.yCard!=null?m.yCard.cost:0)) return;
                p.mana -= m.card.cost; discard.add(m.card); p.hand.remove(m.card);
                p.playBinaryOp(m.target, m.card);
                addLog(p.name+": "+m.card.name+" → "+ownerName(tgtOwner,m.target));
                if (m.yCard!=null){
                    p.mana -= m.yCard.cost; discard.add(m.yCard); p.hand.remove(m.yCard);
                    Complex before = m.target.value;
                    p.feedY(m.target, m.yCard.value);
                    addLog("Y="+m.yCard.value+" uygulandı → "+m.target.value);
                    floats.add(FloatingText.forArea(m.target, "→ "+m.target.value));
                    if (m.target==me.field || m.target==ai.field) onValueChanged(before, m.target.value);
                }
            } else if (m.card.type==CardType.ABILITY){
                if (p.mana < m.card.cost) return;
                p.mana -= m.card.cost; discard.add(m.card); p.hand.remove(m.card);
                p.playAbility(tgtOwner, m.card);
                addLog(p.name+": "+m.card.name+" [Ability] ("+ownerName(tgtOwner,m.target)+")");
            } else {
                if (p.mana < m.card.cost) return;
                p.mana -= m.card.cost; discard.add(m.card); p.hand.remove(m.card);
                Complex before = m.target.value;
                p.playCard(m.target, m.card);
                addLog(p.name+": "+m.card.name+" → "+ownerName(tgtOwner,m.target)+" = "+m.target.value);
                floats.add(FloatingText.forArea(m.target, "→ "+m.target.value));
                if (m.target==me.field || m.target==ai.field) onValueChanged(before, m.target.value);
            }

            checkBomb();
        }

        String ownerName(Player o, Area a){
            if (a==o.field) return o==me?"Senin Sayın":"Rakibin Sayısı";
            if (a==o.craft) return o==me?"Senin Craft":"Rakip Craft";
            return "Alan";
        }

        void onValueChanged(Complex before, Complex after){
            double dBefore = dist(before, target);
            double dAfter  = dist(after,  target);
            double delta = dBefore - dAfter;
            String txt = (delta>0?"+":"") + String.format(Locale.US, "%.2f", delta) + " yakınlık";
            floats.add(FloatingText.forCenter(rPlane, txt));
        }

        void tick(){
            if (transitioning){
                transition -= 0.08;
                if (transition<=0){ screen = nextScreen; transitioning=false; transition=0; }
            } else if (transition<0) transition=0;

            for (int i=anims.size()-1;i>=0;i--){
                if (!anims.get(i).step()) anims.remove(i);
            }
            for (int i=floats.size()-1;i>=0;i--){
                if (!floats.get(i).step()) floats.remove(i);
            }

            if (screen==Screen.GAME && !gameOver){
                aiActTick();
                endIfFinished();
            }
        }

        boolean almostEqual(Complex a, Complex b){ return Math.abs(a.re-b.re)<1e-9 && Math.abs(a.im-b.im)<1e-9; }
        double dist(Complex a, Complex b){ double dr=a.re-b.re, di=a.im-b.im; return Math.hypot(dr,di); }

        void endIfFinished(){
            if (gameOver) return;
            boolean drawEmpty = drawPile.isEmpty();
            boolean handsEmpty = me.hand.isEmpty() && ai.hand.isEmpty();
            boolean noPending = me.field.pending==null && ai.field.pending==null && me.craft.pending==null && ai.craft.pending==null;
            if (drawEmpty && handsEmpty && noPending){
                double dm = dist(me.field.value, target);
                double da = dist(ai.field.value, target);
                if (dm<da) { gameOver=true; endText="KAZANDIN!"; }
                else if (da<dm){ gameOver=true; endText="Rakip kazandı"; }
                else { gameOver=true; endText="Berabere"; }
                addLog("Oyun bitti → "+endText);
                screen = Screen.RESULT;
            }
        }

        void checkBomb(){
            if (almostEqual(me.field.value, bomb)) { gameOver=true; endText="BOMBA! Rakip kazandı"; screen=Screen.RESULT; addLog("BOMBA — SEN"); }
            if (almostEqual(ai.field.value, bomb)) { gameOver=true; endText="BOMBA! Sen kazandın"; screen=Screen.RESULT; addLog("BOMBA — RAKİP"); }
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            try {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                switch (screen){
                    case MENU     -> drawMenu(g2);
                    case HELP     -> drawHelp(g2);
                    case SETTINGS -> drawSettings(g2);
                    case GAME     -> drawGame(g2);
                    case PAUSE    -> { drawGame(g2); drawPauseOverlay(g2); }
                    case RESULT   -> { drawGame(g2); drawResultOverlay(g2); }
                }

                if (transition>0){
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)Math.min(1, transition)));
                    g2.setColor(new Color(0,0,0,220));
                    g2.fillRect(0,0,WIDTH,HEIGHT);
                }

                g2.dispose();
            } catch (Throwable ex) {
                if (timer != null && timer.isRunning()) timer.stop();
                g.setColor(Color.RED);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
                g.drawString("Render error: " + ex.getClass().getSimpleName() + " — " +
                             (ex.getMessage() == null ? "" : ex.getMessage()), 20, 28);
                if (!ERROR_PRINTED) { ex.printStackTrace(); ERROR_PRINTED = true; }
            }
        }

        void drawBackdrop(Graphics2D g){
            GradientPaint gp=new GradientPaint(0,0,new Color(16,18,26),0,HEIGHT,new Color(10,12,18));
            g.setPaint(gp); g.fillRect(0,0,WIDTH,HEIGHT);
        }

        void drawMenu(Graphics2D g){
            drawBackdrop(g);
            title(g, "ComplexClash");
            subtitle(g, "Kompleks sayılarla kart düellosu");
            btnStart.draw(g); btnHost.draw(g); btnJoin.draw(g);
            btnHelp.draw(g); btnSettings.draw(g); btnQuit.draw(g);

            g.setColor(new Color(220,220,240,200));
            if (waitingPeer){
                String s = (netRole==NetRole.HOST) ? "Host: Bağlantı bekleniyor..." : "Join: Sunucuya bağlanılıyor...";
                center(g, s, 620);
            } else if (online){
                String s = (netRole==NetRole.HOST) ? "ONLINE: Host" : "ONLINE: Client";
                center(g, s, 620);
            } else {
                g.drawString("© 2025", 12, HEIGHT-10);
            }
        }

        void drawHelp(Graphics2D g){
            drawBackdrop(g);
            title(g, "Nasıl Oynanır?");
            String text =
                    "Amaç: Hedef kompleks sayıya en yakın olmak. Bomba sayısını yapan kaybeder.\n\n" +
                    "• Her tur 5 mana harcayabilirsin. Temel sayı kartlarının maliyeti 0 olabilir.\n" +
                    "• SAYI kartları → hedef alanın değerini doğrudan o sayıya ayarlar.\n" +
                    "• UNARY kartlar (ör. conj, sqrt) → alanın mevcut değerine tekli işlem uygular.\n" +
                    "• BINARY kartlar (X op Y) → önce op kartını alan üzerine koyarsın,\n" +
                    "  sonra aynı alana bir SAYI kartı atarak Y değerini verirsin.\n" +
                    "• Craft alanı: Birden çok kartı kombine edip sonucu 1 mana ile eline SAYI kartı olarak al.\n\n" +
                    "ONLINE 1v1: Menüden HOST ya da JOIN seç. Host bağlanınca oyun seed ile senkronlanır.\n" +
                    "Kısayollar: ESC/P → Duraklat, R → Yeniden Başlat.";
            drawParagraph(g, text, 120, 170, WIDTH-240, 20);
            Button b = new Button(centerX(220), HEIGHT-100, 220, 48, "GERİ");
            b.draw(g);
        }

        void drawSettings(Graphics2D g){
            drawBackdrop(g);
            title(g, "Ayarlar");
            int x = centerX(640), y=220, lh=44;
            drawSettingLine(g, x, y,     "Deste Kartı", settings.deckCount+"", "Azalt", "Artır");
            drawSettingLine(g, x, y+=lh, "AI Hamle Sayısı/Tur", ""+settings.aiSteps, "Azalt", "Artır");
            drawSettingLine(g, x, y+=lh, "AI Bekleme (ms)", ""+(settings.aiDelayFrames*16), "Azalt", "Artır");
            drawSettingLine(g, x, y+=lh, "Grafik Ölçeği", ""+settings.planeScale, "Azalt", "Artır");
            Button back = new Button(centerX(220), HEIGHT-100, 220, 48, "GERİ");
            back.draw(g);
        }

        void drawGame(Graphics2D g){
            drawBackdrop(g);
            g.setColor(new Color(255,255,255,220)); g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
            g.drawString("Hedef: "+target, 60, 36); g.drawString("Bomba: "+bomb, 300, 36);
            if (online){ g.drawString("ONLINE — " + (netRole==NetRole.HOST?"Host":"Client") + (myTurn?" (Senin tur)":" (Rakip tur)"), 520, 36); }

            drawArea(g, rAiField, ai.field, online?"Rakip Sayısı":"AI Sayısı");
            drawArea(g, rMyField, me.field, "Senin Sayın");
            drawArea(g, rAiCraft, ai.craft, online?"Rakip Craft":"AI Craft");
            drawArea(g, rMyCraft, me.craft, "Senin Craft");

            drawPile(g, rDraw, drawPile.size()); drawDiscard(g, rDiscard, discard.size());
            drawPlane(g, rPlane);

            drawHand(g);
            drawButtons(g);

            for (CardAnim a : anims) a.draw(g);
            for (FloatingText ft : floats) ft.draw(g);
            drawLog(g);

            g.setColor(myTurn?new Color(120,200,140):new Color(200,120,120));
            g.fillRoundRect(WIDTH-240, HEIGHT-72, 180, 36, 10,10);
            g.setColor(Color.WHITE);
            g.drawString(myTurn?"SENİN TURUN":(online?"RAKİP TURU":"AI TURU"), WIDTH-225, HEIGHT-48);
        }

        void drawArea(Graphics2D g, Rectangle r, Area a, String title){
            g.setColor(new Color(0,0,0,120)); g.fillRoundRect(r.x+4,r.y+4,r.width,r.height,12,12);
            g.setColor(new Color(36,40,52)); g.fillRoundRect(r.x,r.y,r.width,r.height,12,12);
            g.setColor(new Color(60,66,84)); g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(r.x,r.y,r.width,r.height,12,12);
            g.setColor(new Color(220,220,240)); g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            g.drawString(title, r.x+10, r.y+18);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 16f));
            String val = a.value==null ? "∅" : a.value.toStringNice();
            g.drawString("Değer: "+val, r.x+10, r.y+42);
            if (a.pending!=null){
                g.setColor(new Color(200,220,255));
                g.drawString("Bekleyen: "+a.pending.name+" (Y bekliyor)", r.x+10, r.y+r.height-12);
            }
        }

        void drawPile(Graphics2D g, Rectangle r, int n){
            g.setColor(new Color(0,0,0,120)); g.fillRoundRect(r.x+3,r.y+3,r.width,r.height,10,10);
            g.setColor(new Color(80,90,110)); g.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(new Color(110,120,140)); g.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(Color.WHITE); g.drawString("ÇEK: "+n, r.x+10, r.y+r.height+16);
        }
        void drawDiscard(Graphics2D g, Rectangle r, int n){
            g.setColor(new Color(0,0,0,120)); g.fillRoundRect(r.x+3,r.y+3,r.width,r.height,10,10);
            g.setColor(new Color(70,60,70)); g.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(new Color(120,100,120)); g.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(Color.WHITE); g.drawString("ATIK: "+n, r.x+10, r.y+r.height+16);
        }

        void drawPlane(Graphics2D g, Rectangle r){
            g.setColor(new Color(24,26,34)); g.fillRect(r.x,r.y,r.width,r.height);
            g.setColor(new Color(44,48,60)); g.setStroke(new BasicStroke(1.5f));
            g.drawRect(r.x,r.y,r.width,r.height);
            int cx=r.x+r.width/2, cy=r.y+r.height/2;
            g.setColor(new Color(80,86,106));
            g.drawLine(r.x,cy,r.x+r.width,cy); g.drawLine(cx,r.y,cx,r.y+r.height);

            int scale = settings.planeScale;
            drawPoint(g, target, cx, cy, scale, new Color(250,220,120), "Hedef");
            drawPoint(g, me.field.value, cx, cy, scale, new Color(120,200,255), "Sen");
            drawPoint(g, ai.field.value, cx, cy, scale, new Color(255,120,140), "Rakip");
        }
        void drawPoint(Graphics2D g, Complex z, int cx,int cy,int scale, Color c, String label){
            int x=cx+(int)Math.round(z.re*scale); int y=cy-(int)Math.round(z.im*scale);
            g.setColor(new Color(0,0,0,120)); g.fillOval(x-5+2,y-5+2,10,10);
            g.setColor(c); g.fillOval(x-5,y-5,10,10); g.setColor(Color.WHITE); g.drawString(label, x+8,y-6);
        }

        void drawHand(Graphics2D g){
            g.setColor(new Color(36,40,52)); g.fillRoundRect(rHandArea.x,rHandArea.y,rHandArea.width,rHandArea.height,12,12);
            g.setColor(new Color(60,66,84)); g.drawRoundRect(rHandArea.x,rHandArea.y,rHandArea.width,rHandArea.height,12,12);
            g.setColor(Color.WHITE); g.drawString("EL (mana "+me.mana+")", rHandArea.x+10, rHandArea.y+18);
            for(int i=0;i<5;i++){
                Rectangle s=rHandSlots.get(i);
                if (i<me.hand.size()) drawCard(g, me.hand.get(i), s, selectedCard==me.hand.get(i));
                else drawEmptySlot(g,s);
            }
        }
        void drawEmptySlot(Graphics2D g, Rectangle s){
            g.setColor(new Color(50,56,72)); g.fillRoundRect(s.x,s.y,s.width,s.height,10,10);
            g.setColor(new Color(80,88,108)); g.drawRoundRect(s.x,s.y,s.width,s.height,10,10);
        }
        void drawCard(Graphics2D g, Card c, Rectangle s, boolean sel){
            g.setColor(new Color(64,68,90)); g.fillRoundRect(s.x,s.y,s.width,s.height,10,10);
            g.setColor(sel?new Color(240,220,140):new Color(110,120,140)); g.drawRoundRect(s.x,s.y,s.width,s.height,10,10);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f)); g.setColor(Color.WHITE); g.drawString(c.name, s.x+8, s.y+22);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f)); g.setColor(new Color(210,210,230));
            drawStringWrapped(g, c.desc, s.x+8, s.y+40, s.width-16, 14);
            g.setColor(new Color(255,255,255,220)); g.drawString("Cost "+c.cost, s.x+8, s.y+s.height-10);
        }
        void drawStringWrapped(Graphics2D g, String text, int x, int y, int w, int lh){
            if (text==null) return;
            for(String part: wrap(text,w,g.getFontMetrics())){ g.drawString(part,x,y); y+=lh; }
        }
        List<String> wrap(String s, int w, FontMetrics fm){
            List<String> out=new ArrayList<>();
            StringBuilder line=new StringBuilder();
            for(String word:s.split(" ")){
                String tryLine=line.length()==0?word:line+" "+word;
                if (fm.stringWidth(tryLine)>w){ out.add(line.toString()); line=new StringBuilder(word); }
                else line=new StringBuilder(tryLine);
            }
            if(line.length()>0) out.add(line.toString());
            return out;
        }

        void drawButtons(Graphics2D g){
            drawButton(g, rEndTurn, myTurn?"End Turn":"Rakip oynuyor…", myTurn);
            boolean canClaim = myTurn && me.mana>=1 && me.craft.value!=null;
            drawButton(g, rClaim, "Claim Craft (1)", canClaim);
            drawButton(g, rHelpBtn, "Kılavuz", true);
            drawButton(g, rPauseBtn, "Duraklat", true);
            g.setColor(new Color(255,255,255,200));
            g.drawString(hint, 60, HEIGHT-108);
        }
        void drawButton(Graphics2D g, Rectangle r, String t, boolean enabled){
            g.setColor(enabled?new Color(80,160,120):new Color(70,80,90));
            g.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(new Color(20,30,36)); g.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
            g.setColor(Color.WHITE); Font old=g.getFont(); g.setFont(old.deriveFont(Font.BOLD, 14f));
            centerIn(g,t,r); g.setFont(old);
        }
        void drawLog(Graphics2D g){
            int w=520, h=120, x=720, y=HEIGHT-260;
            g.setColor(new Color(28,30,40,180)); g.fillRoundRect(x,y,w,h,12,12);
            g.setColor(new Color(70,80,96)); g.drawRoundRect(x,y,w,h,12,12);
            g.setColor(new Color(220,220,240)); g.drawString("Olaylar", x+10, y+18);
            int yy=y+36; g.setColor(new Color(200,200,220));
            for(String s: log){ g.drawString("• "+s, x+12, yy); yy+=16; }
        }
        void addLog(String s){ log.addFirst(s); while(log.size()>LOG_MAX) log.removeLast(); }

        void drawPauseOverlay(Graphics2D g){
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
            g.setColor(new Color(10,12,16)); g.fillRect(0,0,WIDTH,HEIGHT);
            g.setComposite(AlphaComposite.SrcOver);
            title(g, "DURAKLATILDI");
            btnResume.draw(g); btnRestart.draw(g); btnBackToMenu.draw(g);
        }
        void drawResultOverlay(Graphics2D g){
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
            g.setColor(new Color(10,12,16)); g.fillRect(0,0,WIDTH,HEIGHT);
            g.setComposite(AlphaComposite.SrcOver);
            title(g, endText);
            btnRestart.draw(g); btnBackToMenu.draw(g);
        }

        void title(Graphics2D g, String s){
            g.setColor(new Color(0,0,0,120)); g.setFont(g.getFont().deriveFont(Font.BOLD, 56f));
            center(g, s, 142+2); g.setColor(Color.WHITE);
            center(g, s, 142);
        }
        void subtitle(Graphics2D g, String s){ g.setFont(g.getFont().deriveFont(Font.PLAIN, 18f)); g.setColor(new Color(220,220,240)); center(g, s, 176); }
        void center(Graphics2D g, String s, int y){ FontMetrics fm=g.getFontMetrics(); int x=(WIDTH-fm.stringWidth(s))/2; g.drawString(s,x,y); }
        int centerX(int w){ return (WIDTH-w)/2; }
        void drawParagraph(Graphics2D g, String text, int x, int y, int w, int lh){
            g.setColor(new Color(220,220,238));
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 16f));
            for(String line : text.split("\n")){
                for(String part: wrap(line, w, g.getFontMetrics())){ g.drawString(part, x, y); y+=lh; }
                y+=6;
            }
        }
        void drawSettingLine(Graphics2D g, int x, int y, String name, String val, String l, String r){
            g.setColor(new Color(36,40,52)); g.fillRoundRect(x, y-22, 640, 44, 10,10);
            g.setColor(new Color(60,66,84)); g.drawRoundRect(x, y-22, 640, 44, 10,10);
            g.setColor(Color.WHITE); g.drawString(name, x+14, y);
            g.drawString(val, x+360, y);
            g.setColor(new Color(80,160,120)); g.fillRoundRect(x+460, y-18, 70, 32,10,10);
            g.setColor(new Color(80,160,120)); g.fillRoundRect(x+540, y-18, 70, 32,10,10);
            g.setColor(Color.BLACK); g.drawString("-", x+487, y+3);
            g.drawString("+", x+567, y+3);
        }
        void centerIn(Graphics2D g, String s, Rectangle r){
            FontMetrics fm=g.getFontMetrics();
            int x=r.x+(r.width-fm.stringWidth(s))/2;
            int y=r.y+(r.height+fm.getAscent()-fm.getDescent())/2;
            g.drawString(s,x,y);
        }

        @Override public void mouseClicked(MouseEvent e){
            Point p=e.getPoint();

            if (screen==Screen.MENU){
                if (btnStart.contains(p)){ online=false; waitingPeer=false; netRole=NetRole.NONE; buildGameRandom(); startTransitionTo(Screen.GAME); return; }
                if (btnHost.contains(p)){ askHost(); return; }
                if (btnJoin.contains(p)){ askJoin(); return; }
                if (btnHelp.contains(p)){ startTransitionTo(Screen.HELP); return; }
                if (btnSettings.contains(p)){ startTransitionTo(Screen.SETTINGS); return; }
                if (btnQuit.contains(p)){ System.exit(0); }
                return;
            }
            if (screen==Screen.HELP || screen==Screen.SETTINGS){
                startTransitionTo(Screen.MENU); return;
            }
            if (screen==Screen.PAUSE){
                if (btnResume.contains(p)){ screen=Screen.GAME; return; }
                if (btnRestart.contains(p)){ if (online) sendRestart(); buildGameRandom(); screen=Screen.GAME; return; }
                if (btnBackToMenu.contains(p)){ startTransitionTo(Screen.MENU); return; }
                return;
            }
            if (screen==Screen.RESULT){
                if (btnRestart.contains(p)){ if (online) sendRestart(); buildGameRandom(); screen=Screen.GAME; return; }
                if (btnBackToMenu.contains(p)){ startTransitionTo(Screen.MENU); return; }
                return;
            }
            if (screen!=Screen.GAME) return;

            if (rHelpBtn.contains(p)){ startTransitionTo(Screen.HELP); return; }
            if (rPauseBtn.contains(p)){ screen=Screen.PAUSE; return; }
            if (gameOver || !myTurn) return;

            if (rEndTurn.contains(p)){
                selectedCard=null;
                me.endTurnUsed=true; consecutivePasses++; if (consecutivePasses>=2) endIfFinished();
                if (online) netSend("END|"+(netRole==NetRole.HOST?"HOST":"CLIENT"));
                nextTurn(); return;
            }
            if (rClaim.contains(p) && me.craft.value!=null){
                if (me.mana>=1){
                    Complex val=me.craft.value;
                    me.mana-=1;
                    me.hand.add(CardFactory.number("Craft "+val.toShort(), val, 0));
                    me.craft.clear();
                    hint="Craft değeri eline SAYI olarak eklendi";
                    addLog("Craft alındı: "+val);
                    if (online) netSend("CLAIM|"+(netRole==NetRole.HOST?"HOST":"CLIENT"));
                    repaint(); return;
                } else { hint="Yetersiz mana"; return; }
            }
            for (int i=0;i<me.hand.size() && i<rHandSlots.size(); i++){
                if (rHandSlots.get(i).contains(p)){ selectedCard = me.hand.get(i); hint = "Seçildi: "+selectedCard.name+" → bir hedefe tıkla"; repaint(); return; }
            }

            Area clicked = null;
            if (rMyField.contains(p)) clicked=me.field;
            else if (rAiField.contains(p)) clicked=ai.field;
            else if (rMyCraft.contains(p)) clicked=me.craft;
            else if (rAiCraft.contains(p)) clicked=ai.craft;

            if (clicked!=null){
                if (selectedCard==null){ hint="Önce elden kart seç"; return; }

                Player owner = (clicked==me.field||clicked==me.craft)?me:ai;

                if (selectedCard.type==CardType.ABILITY){
                    if (me.mana<selectedCard.cost){ hint="Mana yetmiyor"; return; }
                    Move m=new Move(); m.card=selectedCard; m.target=owner.field;
                    m.handIndex = me.hand.indexOf(selectedCard);
                    playMoveAnimated(me, m, true);
                    selectedCard=null; repaint(); return;
                }

                if (clicked.pending!=null && selectedCard.type==CardType.NUMBER){
                    if (me.mana<selectedCard.cost){ hint="Mana yetmiyor"; return; }
                    Move m=new Move(); m.card=CardFactory.binary("X+Y","",0,(x,y)->x);
                    m.target=clicked; m.yCard=selectedCard;
                    m.handIndex = -1;
                    m.yIndex    = me.hand.indexOf(selectedCard);
                    playMoveAnimated(me,m,true);
                    selectedCard=null; repaint(); return;
                }

                if (selectedCard.type==CardType.BINARY){
                    if (me.mana<selectedCard.cost){ hint="Mana yetmiyor"; return; }
                    String banned = "X+Y";
                    if (selectedCard.name.equals(banned) && me.banned.containsKey(banned)){ hint="Bu operatör 3 tur yasaklı"; return; }
                    Move m=new Move(); m.card=selectedCard; m.target=clicked;
                    m.handIndex = me.hand.indexOf(selectedCard);
                    playMoveAnimated(me,m,true);
                    selectedCard=null; hint="Aynı alana SAYI kartı tıkla (Y)"; repaint(); return;
                }

                if (me.mana<selectedCard.cost){ hint="Mana yetmiyor"; return; }
                Move m=new Move(); m.card=selectedCard; m.target=clicked;
                m.handIndex = me.hand.indexOf(selectedCard);
                playMoveAnimated(me,m,true);
                selectedCard=null; repaint(); return;
            }
        }

        @Override public void mousePressed(MouseEvent e) { }
        @Override public void mouseReleased(MouseEvent e) { }
        @Override public void mouseEntered(MouseEvent e) { }
        @Override public void mouseExited(MouseEvent e) { }

        @Override public void keyTyped(KeyEvent e) {}
        @Override public void keyPressed(KeyEvent e) {
            if (e.getKeyCode()==KeyEvent.VK_ESCAPE || e.getKeyCode()==KeyEvent.VK_P){
                if (screen==Screen.GAME) screen=Screen.PAUSE;
                else if (screen==Screen.PAUSE) screen=Screen.GAME;
            }
            if (e.getKeyCode()==KeyEvent.VK_R){
                if (screen==Screen.GAME || screen==Screen.PAUSE || screen==Screen.RESULT){ buildGameRandom(); screen=Screen.GAME; }
            }
            if (e.getKeyCode()==KeyEvent.VK_H){ startTransitionTo(Screen.HELP); }
        }
        @Override public void keyReleased(KeyEvent e) {}

        void startTransitionTo(Screen s){ nextScreen=s; transitioning=true; transition=1.0; }

        void askHost(){
            String portStr = JOptionPane.showInputDialog(this,"Host Port (ör. 7777):","7777");
            if (portStr==null) return;
            try{
                int port = Integer.parseInt(portStr.trim());
                netRole = NetRole.HOST; waitingPeer = true; online = true;
                new Thread(() -> {
                    try (ServerSocket ss = new ServerSocket(port)){
                        addLog("Host: port "+port+" açıldı, bekleniyor...");
                        Socket s = ss.accept();
                        addLog("Client bağlandı: "+s.getInetAddress());
                        peer = new NetPeer(s, this);
                        waitingPeer = false;
                        long seed = System.currentTimeMillis();
                        hostStarts = true;
                        netSend("SYNC|"+seed+"|1");
                        SwingUtilities.invokeLater(() -> {
                            buildGameWithSeed(seed, true);
                            screen = Screen.GAME;
                        });
                    } catch (IOException ex){
                        online=false; waitingPeer=false; netRole=NetRole.NONE;
                        JOptionPane.showMessageDialog(this, "Host hata: "+ex.getMessage());
                    }
                },"HostThread").start();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this, "Geçersiz port"); }
        }

        void askJoin(){
            String ip = JOptionPane.showInputDialog(this,"Sunucu IP (ör. 127.0.0.1):","127.0.0.1");
            if (ip==null) return;
            String portStr = JOptionPane.showInputDialog(this,"Port (ör. 7777):","7777");
            if (portStr==null) return;
            try{
                int port = Integer.parseInt(portStr.trim());
                netRole = NetRole.CLIENT; waitingPeer = true; online=true;
                new Thread(() -> {
                    try {
                        Socket s = new Socket(ip, port);
                        addLog("Sunucuya bağlandı: "+ip+":"+port);
                        peer = new NetPeer(s, this);
                        waitingPeer = false;
                        SwingUtilities.invokeLater(() -> screen=Screen.MENU);
                    } catch (IOException ex){
                        online=false; waitingPeer=false; netRole=NetRole.NONE;
                        JOptionPane.showMessageDialog(this, "Join hata: "+ex.getMessage());
                    }
                },"JoinThread").start();
            }catch(Exception ex){ JOptionPane.showMessageDialog(this, "Geçersiz port"); }
        }

        void onNetLine(String line){
            try{
                String[] t = line.split("\\|");
                switch(t[0]){
                    case "SYNC" -> {
                        long seed = Long.parseLong(t[1]);
                        boolean hs = "1".equals(t[2]);
                        SwingUtilities.invokeLater(() -> {
                            buildGameWithSeed(seed, hs);
                            screen = Screen.GAME;
                        });
                    }
                    case "MOVE" -> {
                        String side = t[1];
                        char typ = t[2].charAt(0);
                        int handIdx = Integer.parseInt(t[3]);
                        int targetId = Integer.parseInt(t[4]);
                        int yIdx = Integer.parseInt(t[5]);

                        Player p = ai;
                        Area tgt = mapTargetById(targetId);

                        Move m = new Move();
                        m.target = tgt;

                        if (typ=='N' || typ=='U' || typ=='A' || typ=='B'){
                            if (typ!='B' && handIdx>=0 && handIdx<p.hand.size()){
                                m.card = p.hand.get(handIdx);
                                m.handIndex = handIdx;
                            } else if (typ=='B' && handIdx>=0 && handIdx<p.hand.size()){
                                m.card = p.hand.get(handIdx); m.handIndex = handIdx;
                                if (yIdx>=0 && yIdx<p.hand.size()){
                                    m.yCard = p.hand.get(yIdx); m.yIndex = yIdx;
                                }
                            } else {
                                addLog("Uzak MOVE hatalı indeks.");
                                return;
                            }
                            SwingUtilities.invokeLater(() -> playMoveAnimated(p, m, false));
                        }
                    }
                    case "END" -> {
                        SwingUtilities.invokeLater(this::nextTurn);
                    }
                    case "CLAIM" -> {
                        SwingUtilities.invokeLater(() -> {
                            if (ai.craft.value!=null && ai.mana>=1){
                                Complex v = ai.craft.value; ai.mana--; ai.hand.add(CardFactory.number("Craft "+v.toShort(), v, 0)); ai.craft.clear();
                                addLog("Rakip craft aldı: "+v);
                            }
                        });
                    }
                    case "RESTART" -> {
                        SwingUtilities.invokeLater(() -> { buildGameRandom(); screen=Screen.GAME; });
                    }
                }
            }catch(Exception ex){
                addLog("Net parse hata: "+ex.getMessage());
            }
        }

        void onPeerClosed(){
            online=false; waitingPeer=false; netRole=NetRole.NONE; peer=null;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Bağlantı kapandı.");
                screen=Screen.MENU;
            });
        }

        void netSend(String s){ if (peer!=null) peer.send(s); }

        void netSendMove(Move m, int targetId){
            if (peer==null) return;
            char typ = switch(m.card.type){
                case NUMBER -> 'N';
                case UNARY  -> 'U';
                case BINARY -> 'B';
                case ABILITY-> 'A';
            };
            int yIdx = (m.yCard==null)? -1 : m.yIndex;
            int handIdx = m.handIndex;
            String side = (netRole==NetRole.HOST) ? "HOST" : "CLIENT";
            netSend("MOVE|"+side+"|"+typ+"|"+handIdx+"|"+targetId+"|"+yIdx);
        }

        void sendRestart(){ netSend("RESTART|"); }

        int targetIdOf(Area a){
            if (netRole==NetRole.HOST){
                if (a==me.field) return 0;
                if (a==me.craft) return 1;
                if (a==ai.field) return 2;
                if (a==ai.craft) return 3;
            } else {
                if (a==me.field) return 2;
                if (a==me.craft) return 3;
                if (a==ai.field) return 0;
                if (a==ai.craft) return 1;
            }
            return 0;
        }
        Area mapTargetById(int id){
            if (netRole==NetRole.HOST){
                return switch(id){
                    case 0 -> me.field;
                    case 1 -> me.craft;
                    case 2 -> ai.field;
                    case 3 -> ai.craft;
                    default -> me.field;
                };
            } else {
                return switch(id){
                    case 0 -> ai.field;
                    case 1 -> ai.craft;
                    case 2 -> me.field;
                    case 3 -> me.craft;
                    default -> me.field;
                };
            }
        }
    }

    enum NetRole { NONE, HOST, CLIENT }

    static class NetPeer {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final GamePanel gp;

        NetPeer(Socket s, GamePanel gp) throws IOException {
            this.socket = s; this.gp = gp;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(s.getOutputStream(), true);
            Thread t = new Thread(this::readLoop, "NetPeer-Reader"); t.setDaemon(true); t.start();
        }
        void readLoop(){
            try{
                String line;
                while((line=in.readLine())!=null){
                    gp.onNetLine(line);
                }
            }catch(IOException ignored){
            }finally{
                try{ socket.close(); }catch(IOException ignored){}
                gp.onPeerClosed();
            }
        }
        void send(String s){ out.println(s); }
    }

    static class NumberPools{
        static List<Complex> bigPool(){ List<Complex> list=new ArrayList<>();
            for(int k=-30;k<=30;k++){ list.add(new Complex(k,0)); }
            list.add(new Complex(Math.PI,0)); list.add(new Complex(Math.E,0)); list.add(Complex.I); list.add(Complex.I.neg());
            list.add(new Complex(Math.PI/2,0)); list.add(new Complex(Math.sqrt(2),0));
            for(int k=0;k<12;k++){ double ang=2*Math.PI*k/12.0; list.add(Complex.fromPolar(1, ang)); }
            list.add(new Complex(13.37, -6.28));
            return list;
        }
    }

    static class Player {
        final String name; int mana=5; int bonusNext=0; boolean endTurnUsed=false;
        final Area field = new Area();
        final Area craft = new Area();
        final List<Card> hand = new ArrayList<>();
        final Map<String,Integer> banned = new HashMap<>();
        Player(String name){ this.name=name; }
        void drawUpTo(Deque<Card> pile, int n){ while(hand.size()<n && !pile.isEmpty()){ hand.add(pile.pollFirst()); } }
        void newTurn(){ mana = 5 + bonusNext; bonusNext=0; endTurnUsed=false;
            Iterator<Map.Entry<String,Integer>> it=banned.entrySet().iterator();
            while(it.hasNext()){ Map.Entry<String,Integer> e = it.next(); int v=e.getValue()-1; if (v<=0) it.remove(); else e.setValue(v); } }
        void playCard(Area area, Card c){
            if (c.type==CardType.NUMBER){ area.setValue(c.value); }
            else if (c.type==CardType.UNARY){ area.setValue(c.unary.apply(area.value)); }
        }
        void playBinaryOp(Area area, Card c){ area.pending = c; }
        void feedY(Area area, Complex y){ if (area.pending!=null){ area.setValue(area.pending.binary.apply(area.value, y)); area.pending=null; } }
        void playAbility(Player target, Card c){
            switch (c.ability){
                case MANA_BOOST -> target.bonusNext += 3;
                case SWAP_VALUES -> { Complex tmp = target.field.value; target.field.value = this.field.value; this.field.value = tmp; }
                case BAN_ADD -> target.banned.put("X+Y", 3);
            }
        }
    }

    enum Screen { MENU, HELP, SETTINGS, GAME, PAUSE, RESULT }

    static class Settings {
        int deckCount = 120;
        int aiSteps = 3;
        int aiDelayFrames = 30;
        int planeScale = 40;
    }

    interface UnaryOp { Complex apply(Complex x); }
}
// ===================== /scratch_8.java =====================
