package cam72cam.immersiverailroading.script;

import cam72cam.mod.ModCore;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.*;

/**
 * Helper class to construct LuaLibrary
 */
public class LuaLibrary {
    private final HashMap<String, Object[]> functions;
    private final String type;

    private LuaLibrary(String typeName){
        this.type = typeName;
        this.functions = new HashMap<>();
    }

    private LuaLibrary() {
        this.functions = new HashMap<>();
        this.type = null;
    }

    /**
     * A static factory method
     * @param typeName Type of the object or namespace of the library
     * @return The constructed LuaLibrary
     */
    public static LuaLibrary create(String typeName){
        return new LuaLibrary(typeName);
    }

    public static LuaLibrary create() {
        return new LuaLibrary();
    }

    /**
     * Register a function which has no input and output
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, Runnable function){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[0] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[0] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a single parameter consumer function, like setter
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, Consumer<LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[1] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[1] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a double parameter consumer function, like setter
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, BiConsumer<LuaValue, LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[2] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[2] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a Triple parameter consumer function, like setter
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param consumer Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunction(String name, TriConsumer<LuaValue, LuaValue, LuaValue> consumer){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[3] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[3] = consumer;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a zero parameter function with a return value, like getter
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param supplier Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, Supplier<LuaValue> supplier){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[0] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[0] = supplier;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a single parameter function with a return value
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, Function<LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[1] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[1] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a double parameter function with a return value
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, BiFunction<LuaValue, LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[2] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[2] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a triple parameter function with a return value
     * <p>
     * Allow function overload with different parameter count
     * @param name Name of the function
     * @param function Functioning part
     * @return The class itself
     */
    public LuaLibrary addFunctionWithReturn(String name, TriFunction<LuaValue, LuaValue, LuaValue, LuaValue> function){
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[3] != null){
            ModCore.error(String.format("Invalid overload function registry detected within object %s, the latter one won't be registered!", name));
            return this;
        }
        func[3] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a VarArg function, like setter
     * <p>
     * Allows for Functions with Variable parameter counts
     * @param name Name of the function
     * @param function functioning part
     * @return the class itself
     */

    public LuaLibrary addVarArgsFunction(String name, Consumer<Varargs> function) {
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if(func[4] != null) {
            ModCore.error("Invalid overload function registry detected within object %s, the latter one won't be registered!", name);
            return this;
        }
        func[4] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * Register a VarArg function with a return value
     * <p>
     * Allows for Functions with Variable parameter counts
     * @param name Name of the function
     * @param function functioning part
     * @return the class itself
     */

    public LuaLibrary addVarArgsFunctionWithReturn(String name, Function<Varargs, Varargs> function) {
        Object[] func = functions.getOrDefault(name, new Object[5]);
        if (func[4] != null) {
            ModCore.error("Invalid overload function registry detected within object %s, the latter one won't be registered!", name);
            return this;
        }
        func[4] = function;
        functions.put(name, func);
        return this;
    }

    /**
     * A terminal function to set up this object as library in the given globals
     * @param globals The environment
     */
    public void setInGlobals(Globals globals){
        globals.set(this.type, initFunctions(new LuaTable()));
    }

    public LuaTable getAsTable() {
        return initFunctions(new LuaTable());
    }

    /**
     * Internal function to set up functions
     */
    @SuppressWarnings("All")
    private LuaTable initFunctions(LuaTable object){
        for(Map.Entry<String, Object[]> func : functions.entrySet()){
            object.set(func.getKey(), new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    try {
                        int nargs = args.narg();
                        switch (nargs) {
                            case 0:
                                if (func.getValue()[0] instanceof Supplier) {
                                    return ((Supplier<LuaValue>) func.getValue()[0]).get();
                                } else if (func.getValue()[0] instanceof Runnable) {
                                    ((Runnable) func.getValue()[0]).run();
                                    return NIL;
                                }
                            case 1:
                                if (func.getValue()[1] instanceof Function) {
                                    return ((Function<LuaValue, LuaValue>) func.getValue()[1]).apply(args.arg(1));
                                } else if (func.getValue()[1] instanceof Consumer) {
                                    ((Consumer<LuaValue>) func.getValue()[1]).accept(args.arg(1));
                                    return NIL;
                                }
                            case 2:
                                if (func.getValue()[2] instanceof BiFunction) {
                                    return ((BiFunction<LuaValue, LuaValue, LuaValue>) func.getValue()[2])
                                            .apply(args.arg(1), args.arg(2));
                                } else if (func.getValue()[2] instanceof BiConsumer) {
                                    ((BiConsumer<LuaValue, LuaValue>) func.getValue()[2])
                                            .accept(args.arg(1), args.arg(2));
                                    return NIL;
                                }
                            case 3:
                                if (func.getValue()[3] instanceof TriFunction) {
                                    return ((TriFunction<LuaValue, LuaValue, LuaValue, LuaValue>) func.getValue()[3])
                                            .apply(args.arg(1), args.arg(2), args.arg(3));
                                } else if (func.getValue()[3] instanceof TriConsumer) {
                                    ((TriConsumer<LuaValue, LuaValue, LuaValue>) func.getValue()[3])
                                            .accept(args.arg(1), args.arg(2), args.arg(3));
                                    return NIL;
                                }
                            default:
                                if (func.getValue()[4] instanceof Function) {
                                    return ((Function<Varargs, Varargs>) func.getValue()[4]).apply(args);
                                } else if (func.getValue()[4] instanceof Consumer) {
                                    ((Consumer<Varargs>) func.getValue()[4]).accept(args);
                                    return NIL;
                                }
                        }
                    } catch (Exception e) {
                        ModCore.error("Error invoking LuaFunction: " + e.getMessage());
                    }
                    return NIL;
                }
            });
        }
        return object;
    }

    /**
     * A consumer which consumes three parameters
     * @param <I> Parameter No.1's type
     * @param <J> Parameter No.2's type
     * @param <K> Parameter No.3's type
     */
    @FunctionalInterface
    public interface TriConsumer<I,J,K> {
        void accept(I arg1, J arg2, K arg3);
    }

    /**
     * A function which turns three parameters into one return value
     * @param <I> Parameter No.1's type
     * @param <J> Parameter No.2's type
     * @param <K> Parameter No.3's type
     * @param <R> Return value's type
     */
    @FunctionalInterface
    public interface TriFunction<I,J,K,R> {
        R apply(I arg1, J arg2, K arg3);
    }
}
