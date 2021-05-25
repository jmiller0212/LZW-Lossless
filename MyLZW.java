// Jarod Miller (jcm138)

import java.util.Arrays;

/*************************************************************************
 *  Compilation:  javac LZW.java
 *  Execution:    java LZW - < input.txt   (compress)
 *  Execution:    java LZW + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *
 *  Compress or expand binary input from standard input using LZW.
 *
 *  WARNING: STARTING WITH ORACLE JAVA 6, UPDATE 7 the SUBSTRING
 *  METHOD TAKES TIME AND SPACE LINEAR IN THE SIZE OF THE EXTRACTED
 *  SUBSTRING (INSTEAD OF CONSTANT SPACE AND TIME AS IN EARLIER
 *  IMPLEMENTATIONS).
 *
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 *************************************************************************/

public class MyLZW {
    private static final int R = 256;   // number of input chars
    private static int L = 512;         // number of codewords = 2^W
    private static int W = 9;           // codeword width/bit length of the codeword

    public static void compress(String mode) {
        String input = BinaryStdIn.readString();    // this is the entire file going into input
        double processedU = 0;
        double processedC = 0;
        double compressedBitLength = 0;
        double oldRatio = 0.0;
        double newRatio = 0.0;
        double ratioOfRatios = 0.0;
        boolean n = false;
        boolean r = false;
        boolean m = false;
        // int count = 0;

        // what mode are we using and creating the header 
        // at the beginnning of the compressed file
        if(mode.equals("n")) {
            n = true;
            BinaryStdOut.write('n');
        }
        if(mode.equals("r")) {
            r = true;
            BinaryStdOut.write('r');
        }
        if(mode.equals("m")) {
            m = true;
            BinaryStdOut.write('m');
        }
                                                    // a crap ton of ASCII characters
        TST<Integer> st = new TST<Integer>();       // initialize TST as our symbol table
        // put ascii codes into the st
        for(int i = 0; i < R; i++) {                // we will populate TST with our Radix
            st.put("" + (char) i, i);
        }
        int code = R+1;  // R is codeword for EOF

        while(input.length() > 0) {
            // implement reset
            if(code == L && W == 16) {
                if(r == true) {
                    st = new TST<Integer>();
                    for(int i = 0; i < R; i++) {
                        st.put("" + (char) i, i);
                    }
                    L = 512;
                    W = 9;
                    code = R+1;
                }
            }
            if(code >= L && W == 16) {
                if(m == true) {
                    ratioOfRatios = oldRatio / newRatio;
                    newRatio = processedU / processedC;
                    // System.err.println(ratioOfRatios);
                    // count++;
                    if(ratioOfRatios > 1.1) {
                        // reset st
                        st = new TST<Integer>();
                        for(int i = 0; i < R; i++) {
                            st.put("" + (char) i, i);
                        }
                        L = 512;
                        W = 9;
                        code = R+1;
                        // // System.err.println(count);
                        // count = 0;
                    }
                }
            }

            String s = st.longestPrefixOf(input);   // Find max prefix match s.
            BinaryStdOut.write(st.get(s), W);       // Print s's encoding.
            compressedBitLength += W;
            processedC = compressedBitLength / 8;

            int t = s.length();                     // t is the length of the word we just encoded
            processedU += t;

            if(t < input.length() && code < L) {    // Add s to symbol table. How?
                                                    // t < input.length() checks to see if t is less than the rest of the remaining file
                                                    // code < L checks if the code is less than our number of codewords (range of possible values)
                st.put(input.substring(0, t + 1), code++);  // Add codeword and the code associated with it
                                                            // input.substring(0 to t+1) is the length of the word + the next char in the stream (the key in the TST)
                                                            // code++ adds the next entry into the TST (the value)
                // implemented variable-width codewods
                if(code == L && W < 16) {
                    L = 2*L;
                    W++;
                }
            }
            input = input.substring(t); // Scan past s in input.
            // oldRatio goes here
            if(code < L && W <= 16) {
                oldRatio = processedU / processedC;
                newRatio = oldRatio;
                // count++;
            }
        }
        BinaryStdOut.write(R, W);
        compressedBitLength += W;
        processedC = compressedBitLength / 8;
        BinaryStdOut.close();
    }

    public static void expand() {
        String[] st = new String[L];
        int i;                          // next available codeword value, (like code from compression)
        double processedU = 0;          // total amount of uncompressed data processed so far
        double processedC = 0;          // same but compressed
        double compressedBitLength = 0; // this guy adds the Width
        double oldRatio = 0.0;
        double newRatio = 0.0;
        double ratioOfRatios = 0.0;
        boolean n = false;
        boolean r = false;
        boolean m = false;
        // int count = 0;

        // initialize symbol table with all 1-character strings
        for(i = 0; i < R; i++) {
            st[i] = "" + (char) i;
        }
        st[i++] = "";               // (unused) lookahead for EOF
        // implement command line arg for mode
        char mode = BinaryStdIn.readChar();
        if(mode == 'r') {
            r = true;
        }
        if(mode == 'm') {
            m = true;
        }
        // W will need to be resized as compression continues
        int codeword = BinaryStdIn.readInt(W);  // this is our first codeword
        // process compressed
        compressedBitLength = W;
        processedC = compressedBitLength / 8;

        if(codeword == R) {
            return;     // expanded message is empty string
        }
        String val = st[codeword];  // grabs the first character associted with the ascii value
                                    // codewords in expansion are the integers
        while(true) {
            BinaryStdOut.write(val);    // writes the binary representation of the character/string
            // reset codebook
            if(W == 16 && i == L-1) {
                if(r == true) {
                    st = Arrays.copyOfRange(st, 0, R);
                    W = 9;
                    L = 512;
                    st = Arrays.copyOf(st, L);
                    st[R] = "";
                    i = R;
                }
            }
            // processed uncompressed
            int length = val.length();
            processedU += length;
            if(i < L-1 && W <= 16) {
                oldRatio = processedU / processedC;
                newRatio = oldRatio;
                // count++;
            }
            // monitor
            if(i >= L-1 && W == 16) {
                if(m == true) {
                    ratioOfRatios = oldRatio / newRatio;
                    newRatio = processedU / processedC;
                    // System.err.println(ratioOfRatios);
                    // count++;
                    if(ratioOfRatios > 1.1) {
                        st = Arrays.copyOfRange(st, 0, R);
                        W = 9;
                        L = 512;
                        st = Arrays.copyOf(st, L);
                        st[R] = "";
                        i = R;
                        // System.err.println(count);
                        // count = 0;
                    }
                }
            }
            codeword = BinaryStdIn.readInt(W);  // second...third..etc. codeword, an int
            compressedBitLength += W;
            processedC = compressedBitLength / 8;

            if(codeword == R) {
                break;
            }
            String s = st[codeword];    // s is now equal to the string stored in the symbol table at index codeword
            if(i == codeword) {
                s = val + val.charAt(0);   // special case hack
            }
            // variable width codewords
            // changed i < L to i <= L-1 because arrays and the first input
            if(i <= L-1) { // anti (spaghetti code)??
                st[i++] = val + s.charAt(0);    // the next pattern to be stored in the symbol table is
                                                // the current string (val) that we're on and the next character
                                                // in the pattern (s.charAt(0))
                // variable-width codeword resize
                if(i == L-1 && W < 16) {
                    L = 2*L;
                    st = Arrays.copyOf(st, L);
                    W++;
                }
            }
            val = s;   
        }
        // System.err.println(count);
        BinaryStdOut.close();
    }


    public static void main(String[] args) {
        if(args[0].equals("-")) {
            compress(args[1]);
        }
        else if(args[0].equals("+")) {
            expand();
        }
        else {
            throw new IllegalArgumentException("Illegal command line argument");
        }
    }

}