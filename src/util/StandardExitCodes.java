package util;

public final class StandardExitCodes {
    // ******* Developer Note *******
    // Exit Values Constants
    public static final int     NORMAL = 0,
                                ERROR  = 1,
                                FILE   = 2,
                                FATAL  = 10;
    
    public final static void showMessage() {
        System.out.println("Exit values: ");
        System.out.printf("  %2d    %s\n", NORMAL, "Normal exit");
        System.out.printf("  %2d    %s\n", ERROR,  "General user errors");
        System.out.printf("  %2d    %s\n", FILE,   "File/Directory related errors");
        System.out.printf("  %2d    %s\n", FATAL,  "Application fatal/unknown error");
    }
}
