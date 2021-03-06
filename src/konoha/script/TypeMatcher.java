package konoha.script;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

public class TypeMatcher {

	HashMap<String, Type> vars = new HashMap<>();
	GenericType recvType;

	public TypeMatcher() {
	}

	public final void init(Type recvType) {
		if (recvType instanceof GenericType) {
			this.recvType = (GenericType) recvType;
		} else {
			this.recvType = null;
		}
		vars.clear();
	}

	public final void reset() {
		vars.clear();
	}

	public final boolean match(Type p, Type a) {
		return this.match(false, p, a);
	}

	public final boolean match(boolean isTypeParameter, Type p, Type a) {
		if (p instanceof Class<?>) {
			if (!isTypeParameter && a instanceof Class<?>) {
				return ((Class<?>) p).isAssignableFrom((Class<?>) a);
			}
			return p == a;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return match(isTypeParameter, vars.get(name), a);
			}
			if (recvType != null) {
				Type t = recvType.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return match(isTypeParameter, t, a);
				}
			}
			vars.put(name, a);
			return true;
		}
		if (p instanceof ParameterizedType) {
			if (a instanceof GenericType) {
				Type rawtype = ((ParameterizedType) p).getRawType();
				if (!match(true, rawtype, ((GenericType) a).base)) {
					return false;
				}
				Type[] pp = ((ParameterizedType) p).getActualTypeArguments();
				Type[] pa = ((GenericType) a).params;
				if (pp.length != pa.length) {
					return false;
				}
				for (int i = 0; i < pp.length; i++) {
					if (!match(true, pp[i], pa[i])) {
						return false;
					}
				}
				return true;
			}
			Debug.TODO("ParameterizedType %p %s\n", p, a.getClass().getName());
			return false;
		}
		if (p instanceof WildcardType) {
			WildcardType w = (WildcardType) p;
			// Debug.TODO("WildcardType '%s' %s %s\n", w.getTypeName(), p,
			// p.getClass().getName());
			return true;
		}
		if (p instanceof GenericArrayType) {
			Debug.TODO("GenericArrayType %s %s\n", p, p.getClass().getName());
			return false;
		}
		Debug.TODO("unknown %s\n", p.getClass().getName());
		return false;
	}

	public final Type resolve(Type p, Class<?> unresolved) {
		if (p instanceof Class<?>) {
			return p;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return vars.get(name);
			}
			if (recvType != null) {
				Type t = recvType.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return t;
				}
			}
			return unresolved; // not found
		}
		if (p instanceof ParameterizedType) {
			Type rawtype = ((ParameterizedType) p).getRawType();
			Type[] params = ((ParameterizedType) p).getActualTypeArguments().clone();
			for (int i = 0; i < params.length; i++) {
				params[i] = resolve(params[i], Object.class);
			}
			return GenericType.newType((Class<?>) rawtype, params);
		}
		if (p instanceof WildcardType) {
			// WildcardType w = (WildcardType) p;
			// w.getTypeName();
			// Debug.TODO("WildcardType '%s' %s %s\n", w.getTypeName(), p,
			// p.getClass().getName());
			return Object.class;
		}
		if (p instanceof GenericArrayType) {
			Debug.TODO("GenericArrayType %s %s\n", p, p.getClass().getName());
		}
		Debug.TODO("unknown %s\n", p.getClass().getName());
		return unresolved;
	}

}
