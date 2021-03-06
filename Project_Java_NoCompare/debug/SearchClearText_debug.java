import java.util.*;
import java.io.*;

/// 元の文字列（平文）候補を生成し、ハッシュ文字列と比較処理を行うクラス
public class SearchClearText_debug {
    /// 処理済み平文の出力用文字列（デバッグ用）
    private String clearTextList = "";
    private boolean output_clearTextList = true;

    /// ハッシュ処理用クラスのインスタンス
    private ComputeHash computeHash = new ComputeHash();

    /// 各スレッド処理終了時結果文字列
    private String[] resultStr;
    private String target_HashedStr;

    /// 元の文字列の候補
    private byte[][] srcStr;

    /// 元の文字列の候補（代入前）
    private byte[][] chr;

    /// 選択したスレッド数のインデックス番号
    private int selectIndex;

    /// 検索対象文字のコードを格納する配列
    private byte[] targetChars;
    
    /// 検索範囲先頭文字
    private int[][] chrStart;

    /// 検索範囲末尾文字
    private int[][] chrEnd;

    /// 選択したアルゴリズムのインデックス番号
    private int Algorithm_Index;

    public SearchClearText_debug(int alg_index, String targetStr, int strLen, int threadMax, int mode) {
        srcStr = new byte[threadMax][];
        chr = new byte[threadMax][];

        for (int i = 0; i < threadMax; i++) {
            srcStr[i] = new byte[strLen];
            chr[i] = new byte[strLen];
        }

        // 選択したアルゴリズムのインデックス番号
        Algorithm_Index = alg_index;

        // 選択したスレッド数のインデックス番号の指定（配列の選択)
        switch (threadMax) {
        case 1:
            selectIndex = 0;
            break;
        case 2:
            selectIndex = 1;
            break;
        case 4:
            selectIndex = 2;
            break;
        case 8:
            selectIndex = 3;
            break;
        case 16:
            selectIndex = 4;
            break;
        default:
            System.out.println("threadMax = " + Integer.toString(threadMax) +   ", threadMax is Invalid...");
            System.exit(-1);
            break;
        }

        // ハッシュ後の検索対象文字列をセット
        target_HashedStr = targetStr;

        // 検索範囲配列の初期化
        targetChars_Init(mode);
    }

    //-----------------------------------------------------------------------------//
    // 検索対象文字を配列にセットする。
    //-----------------------------------------------------------------------------//
    private void targetChars_Init(int mode) {
        // 検索範囲配列の初期化
        chr_StartEnd_Init();

        switch (mode) {
            case 0: {
                // 英数文字および記号が対象のとき
                //targetChars = new int[0xff - 0x00];
                targetChars = new byte[0x7f - 0x20];

                int i = 0;
                for (byte num = 0x20; num < 0x7f; num++, i++)
                {
                    targetChars[i] = num;
                }
                break;
            }
            case 1: {
                // 英数文字のみが対象のとき
                targetChars = new byte[('9' - '0') + 1 + ('Z' - 'A') + 1 + ('z' - 'a') + 1];

                int i = 0;
                for (byte num = (byte)'0'; num <= (byte)'9'; num++, i++)
                {
                    targetChars[i] = num;
                }

                for (byte num = (byte)'A'; num <= (byte)'Z'; num++, i++)
                {
                    targetChars[i] = num;
                }

                for (byte num = (byte)'a'; num <= (byte)'z'; num++, i++)
                {
                    targetChars[i] = num;
                }
                break;
            }
        }

        // 検索範囲配列の初期化
        chr_StartEnd_Set();
    }

    //-----------------------------------------------------------------------------//
    // 検索範囲配列の初期化
    //-----------------------------------------------------------------------------//
    private void chr_StartEnd_Init() {
        chrStart = new int[5][];
        chrEnd = new int[5][];

        chrStart[0] = new int[1];
        chrStart[1] = new int[2];
        chrStart[2] = new int[4];
        chrStart[3] = new int[8];
        chrStart[4] = new int[16];

        chrEnd[0] = new int[1];
        chrEnd[1] = new int[2];
        chrEnd[2] = new int[4];
        chrEnd[3] = new int[8];
        chrEnd[4] = new int[16];
    }

    //-----------------------------------------------------------------------------//
    // 各スレッドごとの対象範囲のセット
    //-----------------------------------------------------------------------------//
    private void chr_StartEnd_Set() {
        //-------------------------------------------------------------------------//
        // スレッド数が１のときの開始・終了文字
        //-------------------------------------------------------------------------//
        chrStart[0][0] = 0;
        chrEnd[0][0] = targetChars.length;

        //-------------------------------------------------------------------------//
        // スレッド数が２（配列インデックス=1）のときの開始・終了文字
        //-------------------------------------------------------------------------//
        chrStart[1][0] = 0;
        chrStart[1][1] = targetChars.length / 2;
        chrEnd[1][0] = chrStart[1][1];
        chrEnd[1][1] = targetChars.length;

        //-------------------------------------------------------------------------//
        // スレッド数が４（配列インデックス=2）のときの開始・終了文字
        //-------------------------------------------------------------------------//
        chrStart[2][0] = 0;
        chrStart[2][1] = 1 * targetChars.length / 4;
        chrStart[2][2] = 2 * targetChars.length / 4;
        chrStart[2][3] = 3 * targetChars.length / 4;
        chrEnd[2][0] = chrStart[2][1];
        chrEnd[2][1] = chrStart[2][2];
        chrEnd[2][2] = chrStart[2][3];
        chrEnd[2][3] = targetChars.length;

        //-------------------------------------------------------------------------//
        // スレッド数が８（配列インデックス=3）のときの開始・終了文字
        //-------------------------------------------------------------------------//
        chrStart[3][0] = 0;
        chrStart[3][1] = 1 * targetChars.length / 8;
        chrStart[3][2] = 2 * targetChars.length / 8;
        chrStart[3][3] = 3 * targetChars.length / 8;
        chrStart[3][4] = 4 * targetChars.length / 8;
        chrStart[3][5] = 5 * targetChars.length / 8;
        chrStart[3][6] = 6 * targetChars.length / 8;
        chrStart[3][7] = 7 * targetChars.length / 8;
        chrEnd[3][0] = chrStart[3][1];
        chrEnd[3][1] = chrStart[3][2];
        chrEnd[3][2] = chrStart[3][3];
        chrEnd[3][3] = chrStart[3][4];
        chrEnd[3][4] = chrStart[3][5];
        chrEnd[3][5] = chrStart[3][6];
        chrEnd[3][6] = chrStart[3][7];
        chrEnd[3][7] = targetChars.length;

        //-------------------------------------------------------------------------//
        // スレッド数が１６（配列インデックス=4）のときの開始・終了文字
        //-------------------------------------------------------------------------//
        chrStart[4][0] = 0;
        chrStart[4][1] = 1 * targetChars.length / 16;
        chrStart[4][2] = 2 * targetChars.length / 16;
        chrStart[4][3] = 3 * targetChars.length / 16;
        chrStart[4][4] = 4 * targetChars.length / 16;
        chrStart[4][5] = 5 * targetChars.length / 16;
        chrStart[4][6] = 6 * targetChars.length / 16;
        chrStart[4][7] = 7 * targetChars.length / 16;
        chrStart[4][8] = 8 * targetChars.length / 16;
        chrStart[4][9] = 9 * targetChars.length / 16;
        chrStart[4][10] = 10 * targetChars.length / 16;
        chrStart[4][11] = 11 * targetChars.length / 16;
        chrStart[4][12] = 12 * targetChars.length / 16;
        chrStart[4][13] = 13 * targetChars.length / 16;
        chrStart[4][14] = 14 * targetChars.length / 16;
        chrStart[4][15] = 15 * targetChars.length / 16;
        chrEnd[4][0] = chrStart[4][1];
        chrEnd[4][1] = chrStart[4][2];
        chrEnd[4][2] = chrStart[4][3];
        chrEnd[4][3] = chrStart[4][4];
        chrEnd[4][4] = chrStart[4][5];
        chrEnd[4][5] = chrStart[4][6];
        chrEnd[4][6] = chrStart[4][7];
        chrEnd[4][7] = chrStart[4][8];
        chrEnd[4][8] = chrStart[4][9];
        chrEnd[4][9] = chrStart[4][10];
        chrEnd[4][10] = chrStart[4][11];
        chrEnd[4][11] = chrStart[4][12];
        chrEnd[4][12] = chrStart[4][13];
        chrEnd[4][13] = chrStart[4][14];
        chrEnd[4][14] = chrStart[4][15];
        chrEnd[4][15] = targetChars.length;
    }

    //-------------------------------------------------------------------//
    // 開始位置、終了位置の確認
    //-------------------------------------------------------------------//
    private void display_chrStartEnd() {
        System.out.println("");
        System.out.println("targetChars.length = " + targetChars.length);

        for (int rows = 0; rows < chrStart.length; rows++) {
            for (int th = 0; th < chrStart[rows].length; th++) {
                System.out.println("chrStart[" + rows + "][" + th + "] = " + chrStart[rows][th]);
                System.out.println("chrEnd[" + rows + "][" + th + "] = " + chrEnd[rows][th]);
            }
        }

        for (int i = 0; i < targetChars.length; i++) {
            System.out.println("targetChars[" + i + "] = " + String.format("%x", targetChars[i]));
        }
    }

    //-------------------------------------------------------------------//
    // 元の文字列総当たり検索
    //-------------------------------------------------------------------//
    public String Get_ClearText(int threadMax) {
        //-------------------------------------------------------------------------//
        // 平文が""かどうかを判定する。
        //-------------------------------------------------------------------------//
        if (target_HashedStr == computeHash.ComputeHash_Common(Algorithm_Index, "".getBytes())) {
            return "";
        }

        //-------------------------------------------------------------------------//
        // 平文が１文字以上の文字列の場合
        //-------------------------------------------------------------------------//
        String[] resultStr = new String[threadMax];

        //---------------------------------------------------------------------//
        // スレッド生成
        //---------------------------------------------------------------------//

        Collection<Integer> elems = new LinkedList<Integer>();
        for (int i = 0; i < threadMax; ++i) {
            elems.add(i);
        }

        Parallel.For(elems, 
        // The operation to perform with each item
        new Parallel.Operation<Integer>() {

            public void perform(Integer threadNum) {
                // 指定したアルゴリズムにてハッシュ値を生成する。
                if (Get_NextClearText_Group_All(threadNum, srcStr, chr, 0)) {
                    //-----------------------------------------------------------//
                    // 同じハッシュ値が生成できる元の文字列が見つかった場合
                    //-----------------------------------------------------------//
                    String ClearText = "";
                    for (int i = 0; i < srcStr[threadNum].length; i++) {
                        ClearText += (char)srcStr[threadNum][i];
                    }

                    resultStr[threadNum] = ClearText;
                } else {
                    resultStr[threadNum] = "";
                }
            };

        });

        //-------------------------------------------------------------------------//
        // すべてのスレッドが結果を返すまで待機する。
        //-------------------------------------------------------------------------//
        while (true) {
            int resultCount = 0;

            try {
                Thread.sleep(500);
            } catch (Exception e) {

            }

            for (int i = 0; i < threadMax; i++) {
                if (resultStr[i] != null) {
                    if (resultStr[i] != "") {
                        if (output_clearTextList) save_clearTextList();

                        // いずれかのスレッドが文字列を返してきた場合（見つかった場合）
                        return resultStr[i];
                    } else {
                        resultCount++;

                        if (resultCount >= threadMax) {
                            if (output_clearTextList) save_clearTextList();

                            // すべて""だった場合（見つからなかった場合）
                            return null;
                        }
                    }
                }
            }
        }
    }

    //-------------------------------------------------------------------//
    // 検索した平文リストのファイルへの保存
    //-------------------------------------------------------------------//
    private void save_clearTextList() {
        try {
            // FileWriterクラスのオブジェクトを生成する
            FileWriter file = new FileWriter("ClearTextList_" + srcStr[0].length + ".txt");
            // PrintWriterクラスのオブジェクトを生成する
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            
            //ファイルに書き込む
            pw.println(clearTextList);
            
            //ファイルを閉じる
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //-------------------------------------------------------------------//
    // 当該階層の平文候補を生成しハッシュ値と比較する。
    // 見つからなければ次の階層へ。
    //-------------------------------------------------------------------//
    protected boolean Get_NextClearText_Group_All(int threadNum, byte[][] targetArray, byte[][] chr, int i) {
        // 文字列の長さの上限を超えた場合は中止する。
        if (i > chr[threadNum].length - 1) {
            return (false);
        }

        //targetArray[threadNum] = new byte[i + 1];
        targetArray[threadNum] = chr[threadNum];

        // まずは文字列長iの候補をチェック
        for (int index = chrStart[selectIndex][threadNum]; index < chrEnd[selectIndex][threadNum]; index++) {
            chr[threadNum][i] = targetChars[index];
            targetArray[threadNum][i] = chr[threadNum][i];

            // デバッグ用出力
            if (output_clearTextList)
                clearTextList += "\"" + (new String(targetArray[threadNum])) + "\"\r\n";

            // 指定したアルゴリズムにてハッシュ値を生成する。
            if (target_HashedStr == computeHash.ComputeHash_Common(Algorithm_Index, targetArray[threadNum])) {
                return (true);
            }
        }

        // 文字列長i + 1の候補をチェック
        for (int index = chrStart[selectIndex][threadNum]; index < chrEnd[selectIndex][threadNum]; index++) {
            chr[threadNum][i] = targetChars[index];

            if (Get_NextClearText_Group_All_level2(threadNum, targetArray, chr, i + 1)) {
                return (true);
            }
        }

        return (false);
    }

    //-------------------------------------------------------------------//
    // 当該階層の平文候補を生成しハッシュ値と比較する。
    // 見つからなければ次の階層へ。
    //-------------------------------------------------------------------//
    protected boolean Get_NextClearText_Group_All_level2(int threadNum, byte[][] targetArray, byte[][] chr, int i) {
        // 文字列の長さの上限を超えた場合は中止する。
        if (i > chr[threadNum].length - 1) {
            return (false);
        }

        targetArray[threadNum] = new byte[i + 1];

        for (int col = 0; col < i; col++) {
            targetArray[threadNum][col] = chr[threadNum][col];
        }

        // まずは文字列長iの候補をチェック
        for (int index = chrStart[0][0]; index < chrEnd[0][0]; index++) {
            chr[threadNum][i] = targetChars[index];
            targetArray[threadNum][i] = (byte)chr[threadNum][i];

            // デバッグ用出力
            if (output_clearTextList)
                clearTextList += "\"" + (new String(targetArray[threadNum])) + "\"\r\n";

            // 指定したアルゴリズムにてハッシュ値を生成する。
            if (target_HashedStr == computeHash.ComputeHash_Common(Algorithm_Index, targetArray[threadNum])) {
                return (true);
            }
        }

        // 文字列長i + 1の候補をチェック
        for (int index = chrStart[0][0]; index < chrEnd[0][0]; index++) {
            chr[threadNum][i] = targetChars[index];

            if (Get_NextClearText_Group_All_level2(threadNum, targetArray, chr, i + 1)) {
                return (true);
            }
        }

        return (false);
    }

    //-------------------------------------------------------------------//
    // 生成した元の文字列候補を表示する。
    //-------------------------------------------------------------------//
    public void display_ClearText() {
        String[] clearText = new String[srcStr.length];

        for (int thread = 0; thread < srcStr.length; thread++)
        {
            // スレッドごとに途中経過文字列を取得

            clearText[thread] = "";
            for (int i = 0; i < srcStr[thread].length; i++)
            {
                clearText[thread] += (char)srcStr[thread][i];
            }

            if (clearText[thread].indexOf("\0") >= 0)
            {
                // 出力先テキストボックスに出力
                System.out.println("スレッド" + thread + ": (起動待ち)");
            }
            else if (resultStr[thread] == "")
            {
                // 出力先テキストボックスに出力
                System.out.println("スレッド" + thread + ": (処理終了)");
            }
            else
            {
                int threadCount = srcStr.length;
                //double progress = ((double)(srcStr[thread][0] - targetChars[chrStart[selectIndex][thread]]) / (double)(chrEnd[selectIndex][thread] - chrStart[selectIndex][thread])) * 100;
                double progress = ((double)(get_index_targetChars(srcStr[thread][0]) - get_index_targetChars(targetChars[chrStart[selectIndex][thread]])) / (double)(chrEnd[selectIndex][thread] - chrStart[selectIndex][thread])) * 100;

                // 出力先テキストボックスに出力
                System.out.print("スレッド" + thread + "  (" + String.format("%.0f", progress) + "% 終了)  :  " + clearText[thread]);
            }

            if (thread % 2 == 0)
            {
                System.out.print("\t");
            }
            else
            {
                System.out.println();
            }
        }
    }

    //-------------------------------------------------------------------//
    // 指定した文字(byte型)が、targetChars[]の何番目かを調べる
    //-------------------------------------------------------------------//
    private int get_index_targetChars(byte val) {
        int i;

        for (i = 0; i < targetChars.length; i++) {
            if (val == targetChars[i]) {
                return i;
            }
        }

        return 0;
    }
}
