package Phase2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;
import java.io.FileWriter;
import java.io.File;

//pageTableBasePtr -> page table start(always)
//pageTablePtrForIC -> point to location where address for IC is to be given
//genericPageTablePtr -> to calculate address for each instruction(GD, PD, LR, SR, ...)
//pageTablePtrForLoad -> use this while loading the instructions.
public class Phase2
{
    private static final char[][] M = new char[300][4];//M->Memory
    private static int IC;
    private static final char[] IR = new char[4];
    private static boolean C;
    private static final char[] R = new char[4];
    private static String buffer;
    private static FileWriter filewriter ;
    private static boolean flag1;
    private static boolean flag2;
    private static Scanner scanner;
    private static int pageTableBasePtr;
    private static int pageTablePtrForIC;
    private static int genericPageTablePtr;
    private static int pageTablePtrForLoad;
    private static int SI,PI, TI;
    private static final PCB pcb = new PCB();
    private static final ArrayList<Integer> checkRepeatedFrames = new ArrayList<>();
    private static final Random random = new Random();
    private static int TTC,LLC;
    private static boolean flagForIC;
    private static String opcode;
    private static String operand;
    private static boolean terminateExecution = false;
    private static boolean onlyTimeLimitHasOccurred = false;


    private static void INIT()
    {
        for(char[]rowInMemory:M)
            Arrays.fill(rowInMemory,'-');

        Arrays.fill(IR,'-');
        Arrays.fill(R,'-');

        checkRepeatedFrames.clear();

        IC = 0;
        buffer = null;
        SI = PI = -1;
        TI = 0;
        TTC = 0;
        LLC = 0;

        C = false;
        flag1 = false;
        flag2 = false;
        flagForIC = false;

        opcode = operand = "";

        pageTablePtrForLoad = pageTablePtrForIC = genericPageTablePtr = 0;
        onlyTimeLimitHasOccurred = false;

    }

    private static int ALLOCATE()
    {
        int frameNo = random.nextInt(30);
        if(checkRepeatedFrames.contains(frameNo))//FrameNumber has been already used, regenerate other
        {
            do
            {
                frameNo = random.nextInt(30);
            } while (checkRepeatedFrames.contains(frameNo));
        }
        checkRepeatedFrames.add(frameNo);
        return frameNo;
    }

    private static void INITIALIZEPAGETABLE()
    {
        for(int i = pageTableBasePtr ; i < (pageTableBasePtr+10);i++)
        {
            M[i][0] = '0';
            M[i][1] = ' ';
            M[i][2] = M[i][3] = '*';
        }
        pageTablePtrForLoad = genericPageTablePtr = pageTablePtrForIC = pageTableBasePtr;
    }

    private static void UPDATEPAGETABLE(int frameNumber)
    {
        String number = Integer.toString(frameNumber);
        if(opcode.equals("GD") || opcode.equals("SR"))
        {
            M[genericPageTablePtr][0] = '1';
            if(number.length() == 1)
            {
                M[genericPageTablePtr][2] = '0';
                M[genericPageTablePtr][3] = number.charAt(0);
            }
            else
            {
                M[genericPageTablePtr][2] = number.charAt(0);
                M[genericPageTablePtr][3] = number.charAt(1);
            }
        }
        else
        {
            M[pageTablePtrForLoad][0] = '1';
            if (number.length() == 1)
            {
                M[pageTablePtrForLoad][2] = '0';
                M[pageTablePtrForLoad][3] = number.charAt(0);
            } else
            {
                M[pageTablePtrForLoad][2] = number.charAt(0);
                M[pageTablePtrForLoad][3] = number.charAt(1);
            }
            pageTablePtrForLoad++;
        }
    }



    private static void TERMINATE(int number)
    {
        StringBuilder sb = new StringBuilder();
        switch(number)
        {
            case 0:
                sb.append("\tNO ERROR\n");
                break;
            case 1:
                sb.append("\tOUT OF DATA\n");
                break;
            case 2:
                sb.append("\tLINE LIMIT EXCEEDED\n");
                break;
            case 3:
                sb.append("\tTIME LIMIT EXCEEDED\n");
                break;
            case 4:
                sb.append("\tOPERATION CODE ERROR(OPCODE ERROR)\n");
                break;
            case 5:
                sb.append("\tOPERAND ERROR\n");
                break;
            case 6:
                sb.append("\tINVALID PAGE FAULT\n");
                break;
            case 7:
                sb.append("\tTIME LIMIT EXCEEDED ERROR AND OPERATION CODE ERROR(OPCODE ERROR)\n");
                break;
            case 8:
                sb.append("\tTIME LIMIT EXCEEDED ERROR AND OPERAND ERROR\n");
                break;
        }
        if((number == 0 || number == 2 || number == 3) && !onlyTimeLimitHasOccurred)
            Phase2.SIMULATION();//1 cycle used for termination.
        try
        {
            filewriter.write("JOB ID:\t"+pcb.JOBID+"\n");
            filewriter.write(new String(sb));
            filewriter.write("IC:\t"+IC+"\n");
            filewriter.write("IR:\t"+Arrays.toString(IR)+"\n");
            filewriter.write("TTC:\t"+TTC+"\n");
            filewriter.write("LLC:\t"+LLC+"\n");

            filewriter.write("\n\n");
            filewriter.flush();
        }
        catch(Exception e)
        {
            System.out.println("Problem in writing");
            System.exit(1);
        }
        if(!buffer.startsWith("$END"))//Find END as we have terminated the JOB
        {
            while (scanner.hasNextLine())
            {
                buffer = scanner.nextLine();
                if (buffer.startsWith("$END"))
                    break;
            }
        }
        for(int k = 0 ; k < M.length ; k++)
            System.out.println(k+":"+Arrays.toString(M[k]));
        System.out.println("-".repeat(83)+"Job Over"+"-".repeat(83));
        Phase2.INIT();
        flag1=true;
        terminateExecution = true;
    }

    private static void READ()
    {
        genericPageTablePtr = ((pageTableBasePtr)+((Integer.parseInt(operand))/10));
        int pageAllocated= Integer.parseInt("" + M[genericPageTablePtr][2] + M[genericPageTablePtr][3]);;
        buffer = scanner.nextLine();
        if(buffer.startsWith("$END"))
        {
            TERMINATE(1);
            return;
        }
        char[] array = buffer.toCharArray();
        int indexForarray = 0;
        boolean flag = false;
        int startingAddress = pageAllocated * 10;
        int endingAddress = startingAddress + 10;
        for(int i = startingAddress;i < endingAddress ;i++)
        {
            for(int j = 0 ; j < 4;j++)
            {
                M[i][j]=array[indexForarray];
                indexForarray++;
                if(indexForarray >= array.length)
                {
                    flag = true;
                    break;
                }
            }
            if(flag)
                break;
        }
    }

    private static void WRITE()
    {
        if(LLC >= pcb.TLL)
        {
            TERMINATE(2);
            return;
        }
        genericPageTablePtr = ((pageTableBasePtr)+((Integer.parseInt(operand))/10));
        int pageAllocated = Integer.parseInt(""+M[genericPageTablePtr][2]+M[genericPageTablePtr][3]);
        StringBuilder sb = new StringBuilder();
        int startingAddress = (pageAllocated * 10);
        int endingAddress = startingAddress+10;
        for(int i = startingAddress; i < endingAddress;i++)
        {
            for(int j = 0 ; j < 4 ; j++)
            {
                if(M[i][j] == '-')
                    sb.append(" ");
                else
                    sb.append(M[i][j]);
            }
        }
        sb.append("\n");
        try
        {
            filewriter.write(new String(sb));
            filewriter.flush();//After we write using filewriter.write(), if there is any data remaining in buffer, filewriter.flush() forces to write the remaining contents of buffer to file
            LLC++;//Writing done successfully.Increment the line limit counter.
        }
        catch(Exception e)
        {
            System.out.println("Problem in writing inside the file");
            System.exit(1);
        }
    }


    private static void MOS()
    {
        if(TI == 0 && SI==1)
            Phase2.READ();
        else if(TI == 0 && SI==2)
            Phase2.WRITE();
        else if(TI == 0 && SI==3)
        {
            TERMINATE(0);
        }
        else if(TI == 2 && SI==1)
        {
            TERMINATE(3);
        }
        else if(TI == 2 && SI==2)
        {
            Phase2.WRITE();
            TERMINATE(3);
        }
        else if(TI == 2 && SI==3)
        {
            TERMINATE(0);
        }
        else if(TI == 0 && PI ==1)
            TERMINATE(4);
        else if(TI == 0 && PI ==2)
            TERMINATE(5);
        else if(TI == 0 && PI ==3)
        {
            if(opcode.equals("GD") || opcode.equals("SR"))//Valid Page Fault
            {
                int frameNumber = ALLOCATE();
                Phase2.UPDATEPAGETABLE(frameNumber);
                return;
            }
            TERMINATE(6);//Invalid page fault
        }
        else if(TI == 2 && PI ==1)
            TERMINATE(7);
        else if(TI == 2 && PI ==2)
            TERMINATE(8);
        else if(TI == 2 && PI ==3)
            TERMINATE(3);
        else if(TI == 2)//Only TI = 2 it means that Time Limit has occurred and the next Instruction after Time Limit is not GD, PD, H.It means time limit occurred and next instruction is LR, SR, CR, BT(Eg last JOB from Input)
        {
            onlyTimeLimitHasOccurred = true;
            TERMINATE(3);
        }
    }

    private static boolean isNumeric(String number)
    {
        try
        {
            Integer.parseInt(number);
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    private static int ADDRESSMAP(String VA)
    {
        if(flagForIC)//For IC
        {
            int frameNo;
            int RA;
            if (isNumeric(VA))
            {
                if(M[pageTablePtrForIC][0] == '1')
                {
                    frameNo = Integer.parseInt("" + M[pageTablePtrForIC][2] + M[pageTablePtrForIC][3]);
                    pageTablePtrForIC++;
                    RA = frameNo * 10;
                    return RA;
                }
                else
                {
                    PI = 3;
                    Phase2.MOS();//Invalid Page Fault
                    PI = -1;
                    return Integer.MIN_VALUE;
                }
            }
            else
            {
                PI = 2;
                Phase2.MOS();//Operand Error
                return Integer.MIN_VALUE;
            }
        }
        else//For Other Instructions
        {
            int frameNo, RA;
            if(isNumeric(VA))
            {
                genericPageTablePtr = (pageTableBasePtr + (Integer.parseInt(VA) / 10));
                if(M[genericPageTablePtr][0] == '1')
                {

                    frameNo = Integer.parseInt(""+M[genericPageTablePtr][2]+M[genericPageTablePtr][3]);
                    RA = ((frameNo * 10) + (Integer.parseInt(VA) % 10));
                    if(opcode.equals("BT"))//adjust indexForIC
                        pageTablePtrForIC = (genericPageTablePtr+1);
                    return RA;
                }
                else//Page Fault has Occurred
                {
                        PI = 3;
                        Phase2.MOS();//Check validPageFault or not
                        PI = -1;
                        if(!terminateExecution)
                        {
                            frameNo = Integer.parseInt(""+M[genericPageTablePtr][2]+M[genericPageTablePtr][3]);
                            RA = ((frameNo * 10) + (Integer.parseInt(VA) % 10));
                            return RA;
                        }
                        return Integer.MIN_VALUE;
                }
            }
            else
            {
                PI = 2;
                Phase2.MOS();//Operand Error
                return Integer.MIN_VALUE;
            }
        }
    }
    //return Integer.MIN_VALUE if error occurs


    private static boolean errorHasOccurredOrNot(int addressReturnedByAddressMap)
    {
        if(addressReturnedByAddressMap == Integer.MIN_VALUE)//If the value ReturnedByAddress Map is Integer.MIN_VALUE, error has occurred.Stop Execution
            return true;

        return false;
    }

    private static void SIMULATION()
    {
        TTC++;
        if(TTC == pcb.TTL)
            TI = 2;
    }


    private static void EXECUTEUSERPROGRAM()
    {
        while(true)
        {
            int RA = Integer.MIN_VALUE;//Random Initialization to run away from compile time error
            flagForIC = true;
            IC = Phase2.ADDRESSMAP(Integer.toString(IC));
            flagForIC = false;
            if(errorHasOccurredOrNot(IC))
                return;
            for(int i = 0;i < 10;i++)
            {

                for (int j = 0; j < 4; j++)
                    IR[j] = M[IC][j];
                IC++;

                if (IR[1] == ' ')
                    opcode = "" + IR[0];
                else
                    opcode = "" + IR[0] + IR[1];

                operand="";
                if (IR[1] != ' ')
                    operand = "" + IR[2] + IR[3];

                //Check operand to know if the pageFault Occurs is valid or not
                if(!opcode.equals("H"))
                {
                    RA = Phase2.ADDRESSMAP(operand);
                    if (Phase2.errorHasOccurredOrNot(RA))
                        return;
                }
                if(TI == 2 &&   (!(opcode.equals("GD") || opcode.equals("PD") || opcode.equals("H"))))//Time Limit has occurred, check if the instruction next is GD, PD, or H then only execute, else stop execution
                {
                    //TI has occurred, check if the next Instruction also has opcode error or not.If opcode error, set PI = 1 also
                    if(!(opcode.equals("SR") || opcode.equals("LR") || opcode.equals("BT") || opcode.equals("CR")))
                        PI = 1;
                    Phase2.MOS();
                    return;
                }
                switch(opcode)
                {
                    case "LR":
                        for(int j = 0;j < 4;j++)
                            R[j] = M[RA][j];
                        break;
                    case "SR":
                        for(int j = 0 ; j < 4 ;j++)
                            M[RA][j] = R[j];
                        break;
                    case "CR":
                        C = false;
                        int count = 0;
                        for (int j = 0; j < 4; j++)
                        {
                            if (M[RA][j] != R[j])
                                break;
                            else
                                count++;
                        }
                        if(count == 4)
                            C = true;
                        break;
                    case "BT":
                        if (C)
                        {
                            IC = RA;
                        }
                        break;
                    case "GD":
                        SI = 1;
                        Phase2.MOS();
                        SI=-1;
                        if(terminateExecution)
                            return;
                        break;
                    case "PD":
                        SI = 2;
                        Phase2.MOS();
                        SI=-1;
                        if(terminateExecution)
                            return;
                        break;
                    case "H":
                        SI = 3;
                        Phase2.MOS();
                        SI=-1;
                        return;
                    default:
                        PI=1;
                        Phase2.MOS();
                        return;
                }
                Phase2.SIMULATION();
            }
        }
    }

    private static void STARTEXECUTION()
    {
        IC = 0;
        Phase2.EXECUTEUSERPROGRAM();
    }

    private static void LOAD()
    {
        while(scanner.hasNextLine())
        {
            buffer = scanner.nextLine();
            String first4CharOfInputLine = buffer.substring(0, 4);
            if (first4CharOfInputLine.equals("$AMJ"))
            {
                pcb.JOBID = Integer.parseInt(buffer.substring(4,8));
                pcb.TTL=Integer.parseInt(buffer.substring(8,12));
                pcb.TLL = Integer.parseInt(buffer.substring(12,16));
                pageTableBasePtr = (Phase2.ALLOCATE() * 10);
                Phase2.INITIALIZEPAGETABLE();
                flag1 = true;
            }
            else if (first4CharOfInputLine.equals("$DTA"))
            {
                Phase2.STARTEXECUTION();
                terminateExecution = false;
                flag1 = true;
            }
            if (!flag1)
            {
                int frameNumber = ALLOCATE();
                Phase2.UPDATEPAGETABLE(frameNumber);
                char[] arrayOfBuffer = buffer.toCharArray();
                int indexForArrayOfBuffer = 0;
                int i = (frameNumber * 10), j;
                while(true)
                {
                    for (j = 0; j < 4; j++)
                    {
                        M[i][j] = arrayOfBuffer[indexForArrayOfBuffer];
                        indexForArrayOfBuffer++;
                        if (indexForArrayOfBuffer >= arrayOfBuffer.length)//Particular Program Card has end
                        {
                            flag2 = true;
                            break;
                        }
                    }
                    if (flag2)
                        break;
                    i++;
                }
            }
            flag2 = false;
            flag1 = false;
        }
    }

    public static void main(String[] args)
    {
        File file = new File("E:\\Programming\\OSProject\\src\\Phase2\\OSInputFile1Phase2.txt");

        try
        {
            scanner = new Scanner(file);
        }
        catch(Exception e)
        {
            System.out.println("Problem in opening input file");
            System.exit(1);
        }

        try
        {
            filewriter = new FileWriter("E:\\Programming\\OSProject\\src\\Phase2\\OSOutputFile1Phase2.txt");
        }
        catch(Exception e)
        {
            System.out.println("Problem in opening output file");
            System.exit(1);
        }

        Phase2.INIT();
        Phase2.LOAD();


        try
        {
            filewriter.close();
        }
        catch(Exception e)
        {
            System.out.println("Problem in closing output file");
        }
    }
}