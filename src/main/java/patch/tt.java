package patch;

import net.minecraft.world.food.IFoodNative;
import org.lime.system;

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

class tt {
    public interface IAction extends Serializable { void action(); }
    public interface IFunc<T> extends Serializable { void func(T val); }

    public static SerializedLambda infoFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            return (SerializedLambda)m.invoke(lambda);
        } catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void print(SerializedLambda lambda) {
        infoFromLambda(system.action(IFoodNative::readNativeSaveData));
        //System.out.println();
        /*
            case REF_getField         -> "getField";
            case REF_getStatic        -> "getStatic";
            case REF_putField         -> "putField";
            case REF_putStatic        -> "putStatic";
            case REF_invokeVirtual    -> "invokeVirtual";
            case REF_invokeStatic     -> "invokeStatic";
            case REF_invokeSpecial    -> "invokeSpecial";
            case REF_newInvokeSpecial -> "newInvokeSpecial";
            case REF_invokeInterface  -> "invokeInterface";
        */
    }

    public static void printAction(IAction action) {
        print(infoFromLambda(action));
    }
    public static <T>void printFunc(IFunc<T> func) {
        print(infoFromLambda(func));
    }
    public static void printInfo(IAction action) {
        print(infoFromLambda(action));
    }

    public static void a1(){}
    public void a2(){}

    public interface aaa { void a3(); }


    public static void main(String[] args) {
        printAction(tt::a1);
        tt.printFunc(tt::a2);
        printFunc(tt.aaa::a3);
    }
}


