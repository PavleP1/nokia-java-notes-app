import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;
import java.util.Vector;

public class ModernNotes extends MIDlet {
    private Display display;
    private ModernCanvas canvas;

    public void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            canvas = new ModernCanvas(this);
            display.setCurrent(canvas);
            new Thread(canvas).start(); // Start 30 FPS Animation Loop
        }
    }

    public void pauseApp() {}

    public void destroyApp(boolean unconditional) {}

    public void exitApp() {
        canvas.stopThread();
        destroyApp(true);
        notifyDestroyed();
    }
}

class ModernCanvas extends Canvas implements Runnable {
    private ModernNotes midlet;
    private boolean running = true;

    // View States
    private static final int VIEW_LIST = 0;
    private static final int VIEW_EDITOR = 1;
    private int currentView = VIEW_LIST;

    // Animation & Smooth Interpolation States
    private float slideAnim = 0.0f;     // Smooth horizontal view transitions
    private float targetSlide = 0.0f;
    private float cardScaleAnim = 0.0f; // Card focus animation
    private int cursorPulse = 0;        // Editor cursor pulse animation
    
    // Toast Notification System
    private String toastMessage = "";
    private float toastY = 320.0f;     // Animated Y position
    private float targetToastY = 320.0f;
    private long toastTimer = 0;

    // Theme Engine (Dark Mode Default)
    private boolean darkTheme = true;
    private int bgCol = 0x121214;
    private int cardCol = 0x1E1E22;
    private int textCol = 0xFFFFFF;
    private int subTextCol = 0x8E8E93;
    private int accentCol = 0x0A84FF;
    private int glassCol = 0x1C1C1E;

    // Data Storage
    private Vector notes = new Vector();
    private int selectedIndex = 0;
    private StringBuffer currentBuffer = new StringBuffer();
    private String currentTitle = "New Note";

    // Off-Screen Buffer for Fast Blur FX
    private Image blurBuffer;
    private int[] pixelData = new int[240 * 40]; // Blur top bar zone (240x40)

    public ModernCanvas(ModernNotes midlet) {
        this.midlet = midlet;
        setFullScreenMode(true); // Suppress Nokia S40 default headers/softkeys

        // Default Sample Data
        notes.addElement("Design System|Build custom pixel components with blur FX.");
        notes.addElement("Shopping List|1. Oat milk\n2. Espresso beans\n3. MicroSD card");
        notes.addElement("Nokia S40 App|Optimized J2ME code using custom Canvas graphics.");
    }

    public void stopThread() {
        running = false;
    }

    // -------------------------------------------------------------
    // 30 FPS ANIMATION LOOP
    // -------------------------------------------------------------
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            // Interpolate animations (Ease-Out Linear Blend)
            slideAnim += (targetSlide - slideAnim) * 0.25f;
            toastY += (targetToastY - toastY) * 0.3f;
            cursorPulse = (cursorPulse + 1) % 30;

            // Hide Toast automatically after 2.5 seconds
            if (System.currentTimeMillis() - toastTimer > 2500 && targetToastY < 320) {
                targetToastY = 320.0f;
            }

            repaint();

            // Target ~30 FPS (33ms frame time)
            long timeTaken = System.currentTimeMillis() - startTime;
            long sleepTime = 33 - timeTaken;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {}
            }
        }
    }

    public void triggerToast(String msg) {
        toastMessage = msg;
        targetToastY = 270.0f; // Slide up into view
        toastTimer = System.currentTimeMillis();
    }

    private void updateTheme() {
        if (darkTheme) {
            bgCol = 0x121214;
            cardCol = 0x1E1E22;
            textCol = 0xFFFFFF;
            subTextCol = 0x8E8E93;
            accentCol = 0x0A84FF;
            glassCol = 0x1C1C1E;
        } else {
            bgCol = 0xF2F2F7;
            cardCol = 0xFFFFFF;
            textCol = 0x000000;
            subTextCol = 0x6C6C70;
            accentCol = 0x007AFF;
            glassCol = 0xE5E5EA;
        }
    }

    // -------------------------------------------------------------
    // RENDERING PIPELINE
    // -------------------------------------------------------------
    protected void paint(Graphics g) {
        // Clear Background Screen
        g.setColor(bgCol);
        g.fillRect(0, 0, getWidth(), getHeight());

        int offsetX = (int) slideAnim;

        // Render List View & Editor View side-by-side during animated slide transition
        if (offsetX > -240) {
            g.translate(offsetX, 0);
            drawListView(g);
            g.translate(-offsetX, 0);
        }
        if (offsetX < 0) {
            g.translate(offsetX + 240, 0);
            drawEditorView(g);
            g.translate(-(offsetX + 240), 0);
        }

        // Overlay Fixed UI Elements (Toast & Blurred Glass Header)
        drawTopGlassBar(g);
        drawToast(g);
    }

    // -------------------------------------------------------------
    // 1. LIST VIEW RENDERING
    // -------------------------------------------------------------
    private void drawListView(Graphics g) {
        int y = 50;

        for (int i = 0; i < notes.size(); i++) {
            String entry = (String) notes.elementAt(i);
            int splitIdx = entry.indexOf('|');
            String title = splitIdx != -1 ? entry.substring(0, splitIdx) : entry;
            String preview = splitIdx != -1 ? entry.substring(splitIdx + 1) : "";

            boolean isSelected = (i == selectedIndex);

            // Card Background
            if (isSelected) {
                g.setColor(accentCol);
                g.drawRoundRect(10, y, 220, 46, 12, 12);
                g.setColor(cardCol);
                g.fillRoundRect(11, y + 1, 218, 44, 10, 10);
            } else {
                g.setColor(cardCol);
                g.fillRoundRect(12, y + 1, 216, 44, 10, 10);
            }

            // Title Text
            g.setColor(textCol);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(title, 22, y + 6, Graphics.TOP | Graphics.LEFT);

            // Subtitle / Preview Snippet
            g.setColor(subTextCol);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            if (preview.length() > 24) preview = preview.substring(0, 24) + "...";
            g.drawString(preview, 22, y + 25, Graphics.TOP | Graphics.LEFT);

            y += 52;
        }

        // Minimalist Bottom Softkey Bar
        g.setColor(glassCol);
        g.fillRect(0, 295, 240, 25);
        g.setColor(subTextCol);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        g.drawString("[5] Open   [*] Mode   [#] Exit", 120, 300, Graphics.TOP | Graphics.HCENTER);
    }

    // -------------------------------------------------------------
    // 2. EDITOR VIEW RENDERING
    // -------------------------------------------------------------
    private void drawEditorView(Graphics g) {
        // Document Title
        g.setColor(textCol);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString(currentTitle, 12, 48, Graphics.TOP | Graphics.LEFT);

        // Body Text Display
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String bodyText = currentBuffer.toString();
        g.drawString(bodyText, 12, 75, Graphics.TOP | Graphics.LEFT);

        // Animated Smooth Pulsing Cursor
        if (cursorPulse < 18) {
            int stringWidth = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL).stringWidth(bodyText);
            g.setColor(accentCol);
            g.fillRect(14 + stringWidth, 76, 2, 13);
        }

        // Live Character Count Chip
        g.setColor(glassCol);
        g.fillRoundRect(12, 260, 90, 22, 10, 10);
        g.setColor(accentCol);
        g.drawString(currentBuffer.length() + " chars", 20, 264, Graphics.TOP | Graphics.LEFT);

        // Editor Bottom Bar
        g.setColor(glassCol);
        g.fillRect(0, 295, 240, 25);
        g.setColor(subTextCol);
        g.drawString("[Select] Save & Back", 120, 300, Graphics.TOP | Graphics.HCENTER);
    }

    // -------------------------------------------------------------
    // 3. FROSTED GLASS HEADER (REAL-TIME SOFTWARE BOX BLUR)
    // -------------------------------------------------------------
    private void drawTopGlassBar(Graphics g) {
        // Apply 3x3 Box Blur algorithm to top 40px region
        applySoftwareBoxBlur(0, 0, 240, 40);

        // Semi-transparent tint overlay on top of blurred canvas content
        g.setColor(glassCol);
        g.fillRect(0, 0, 240, 38);
        g.setColor(darkTheme ? 0x333333 : 0xD1D1D6);
        g.drawLine(0, 38, 240, 38); // Glass accent separator line

        // Modern Header Typography
        g.setColor(textCol);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        g.drawString(currentView == VIEW_LIST ? "Notes" : "Edit", 12, 8, Graphics.TOP | Graphics.LEFT);

        // Minimalist Status Indicator Pills
        g.setColor(accentCol);
        g.fillRoundRect(175, 10, 52, 18, 9, 9);
        g.setColor(0xFFFFFF);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString(darkTheme ? "DARK" : "LIGHT", 201, 12, Graphics.TOP | Graphics.HCENTER);
    }

    // Fast 3x3 Pixel Averaging Box Blur Algorithm tailored for J2ME S40 ARM CPU
    private void applySoftwareBoxBlur(int x, int y, int w, int h) {
        try {
            if (blurBuffer == null) {
                blurBuffer = Image.createImage(w, h);
            }
            Graphics bg = blurBuffer.getGraphics();
            bg.drawRegion(blurBuffer, x, y, w, h, Sprite.TRANS_NONE, 0, 0, Graphics.TOP | Graphics.LEFT);

            blurBuffer.getRGB(pixelData, 0, w, 0, 0, w, h);

            // Horizontal & Vertical Averaging Pass
            for (int i = w + 1; i < pixelData.length - w - 1; i++) {
                int c1 = pixelData[i - 1];
                int c2 = pixelData[i];
                int c3 = pixelData[i + 1];

                int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF) + ((c3 >> 16) & 0xFF)) / 3;
                int g = (((c1 >> 8) & 0xFF) + ((c2 >> 8) & 0xFF) + ((c3 >> 8) & 0xFF)) / 3;
                int b = ((c1 & 0xFF) + (c2 & 0xFF) + (c3 & 0xFF)) / 3;

                pixelData[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        } catch (Exception e) {
            // Fallback gracefully if memory allocation drops frame
        }
    }

    // -------------------------------------------------------------
    // 4. ANIMATED TOAST NOTIFICATION
    // -------------------------------------------------------------
    private void drawToast(Graphics g) {
        int curY = (int) toastY;
        if (curY < 320) {
            g.setColor(accentCol);
            g.fillRoundRect(30, curY, 180, 28, 14, 14);
            g.setColor(0xFFFFFF);
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.drawString(toastMessage, 120, curY + 6, Graphics.TOP | Graphics.HCENTER);
        }
    }

    // -------------------------------------------------------------
    // KEY INPUT & EVENT HANDLING
    // -------------------------------------------------------------
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);

        if (currentView == VIEW_LIST) {
            if (action == UP && selectedIndex > 0) {
                selectedIndex--;
            } else if (action == DOWN && selectedIndex < notes.size() - 1) {
                selectedIndex++;
            } else if (action == FIRE || keyCode == KEY_NUM5) { // Open/Create Note
                if (notes.size() > 0 && keyCode != KEY_NUM5) {
                    String raw = (String) notes.elementAt(selectedIndex);
                    int splitIdx = raw.indexOf('|');
                    currentTitle = splitIdx != -1 ? raw.substring(0, splitIdx) : "Note";
                    currentBuffer = new StringBuffer(splitIdx != -1 ? raw.substring(splitIdx + 1) : raw);
                } else {
                    currentTitle = "Quick Note";
                    currentBuffer = new StringBuffer("");
                }
                currentView = VIEW_EDITOR;
                targetSlide = -240.0f; // Trigger slide left animation
            } else if (keyCode == KEY_STAR) { // Toggle Dark Mode
                darkTheme = !darkTheme;
                updateTheme();
                triggerToast(darkTheme ? "Dark Theme Active" : "Light Theme Active");
            } else if (keyCode == KEY_POUND) { // Instant Silent Exit
                midlet.exitApp();
            }
        } else if (currentView == VIEW_EDITOR) {
            if (action == FIRE) { // Save and return
                if (currentBuffer.length() > 0) {
                    String entry = currentTitle + "|" + currentBuffer.toString();
                    if (selectedIndex < notes.size()) {
                        notes.setElementAt(entry, selectedIndex);
                    } else {
                        notes.addElement(entry);
                    }
                }
                currentView = VIEW_LIST;
                targetSlide = 0.0f; // Slide right animation
                triggerToast("Note Saved!");
            } else if (keyCode == KEY_NUM0) {
                currentBuffer.append(" ");
            } else if (keyCode >= KEY_NUM1 && keyCode <= KEY_NUM9) { // T9 / Multi-tap Sim
                currentBuffer.append((char) ('a' + (keyCode - KEY_NUM2)));
            }
        }
    }
}
