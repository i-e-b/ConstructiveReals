// Copyright (c) 1999, Silicon Graphics, Inc. -- ALL RIGHTS RESERVED 
// 
// Permission is granted free of charge to copy, modify, use and distribute
// this software  provided you include the entirety of this notice in all
// copies made.
// 
// THIS SOFTWARE IS PROVIDED ON AN AS IS BASIS, WITHOUT WARRANTY OF ANY
// KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, WITHOUT LIMITATION,
// WARRANTIES THAT THE SUBJECT SOFTWARE IS FREE OF DEFECTS, MERCHANTABLE, FIT
// FOR A PARTICULAR PURPOSE OR NON-INFRINGING.   SGI ASSUMES NO RISK AS TO THE
// QUALITY AND PERFORMANCE OF THE SOFTWARE.   SHOULD THE SOFTWARE PROVE
// DEFECTIVE IN ANY RESPECT, SGI ASSUMES NO COST OR LIABILITY FOR ANY
// SERVICING, REPAIR OR CORRECTION.  THIS DISCLAIMER OF WARRANTY CONSTITUTES
// AN ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY SUBJECT SOFTWARE IS
// AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
// 
// UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER TORT (INCLUDING,
// WITHOUT LIMITATION, NEGLIGENCE OR STRICT LIABILITY), CONTRACT, OR
// OTHERWISE, SHALL SGI BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL,
// INCIDENTAL, OR CONSEQUENTIAL DAMAGES OF ANY CHARACTER WITH RESPECT TO THE
// SOFTWARE INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF GOODWILL, WORK
// STOPPAGE, LOSS OF DATA, COMPUTER FAILURE OR MALFUNCTION, OR ANY AND ALL
// OTHER COMMERCIAL DAMAGES OR LOSSES, EVEN IF SGI SHALL HAVE BEEN INFORMED OF
// THE POSSIBILITY OF SUCH DAMAGES.  THIS LIMITATION OF LIABILITY SHALL NOT
// APPLY TO LIABILITY RESULTING FROM SGI's NEGLIGENCE TO THE EXTENT APPLICABLE
// LAW PROHIBITS SUCH LIMITATION.  SOME JURISDICTIONS DO NOT ALLOW THE
// EXCLUSION OR LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THAT
// EXCLUSION AND LIMITATION MAY NOT APPLY TO YOU.
// 
// These license terms shall be governed by and construed in accordance with
// the laws of the United States and the State of California as applied to
// agreements entered into and to be performed entirely within California
// between California residents.  Any litigation relating to these license
// terms shall be subject to the exclusive jurisdiction of the Federal Courts
// of the Northern District of California (or, absent subject matter
// jurisdiction in such courts, the courts of the State of California), with
// venue lying exclusively in Santa Clara County, California. 

// Added valueOf(string, radix), fixed some documentation comments.
//		Hans_Boehm@hp.com 1/12/2001
// Fixed a serious typo in inv_CR():  For negative arguments it produced
// 		the wrong sign.  This affected the sign of divisions with
//		negative divisors.  Hans_Boehm@hp.com 8/13/2001

package com.sgi.math;

import java.math.BigInteger;

/**
* Constructive real numbers, also known as recursive, or computable reals.
* Each recursive real number is represented as an object that provides an
* approximation function for the real number.
* The approximation function guarantees that the generated approximation
* is accurate to the specified precision.
* Arithmetic operations on constructive reals produce new such objects;
* they typically do not perform any real computation.
* In this sense, arithmetic computations are exact: They produce
* a description which describes the exact answer, and can be used to
* later approximate it to arbitrary precision.
* <P>
* When approximations are generated, <I>e.g.</i> for output, they are
* accurate to the requested precision; no cumulative rounding errors
* are visible.
* In order to achieve this precision, the approximation function will often
* need to approximate subexpressions to greater precision than was originally
* demanded.  Thus the approximation of a constructive real number
* generated through a complex sequence of operations may eventually require
* evaluation to very high precision.  This usually makes such computations
* prohibitively expensive for large numerical problems.
* But it is perfectly appropriate for use in a desk calculator,
* for small numerical problems, for the evaluation of expressions
* computated by a symbolic algebra system, for testing of accuracy claims
* for floating point code on small inputs, or the like.
* <P>
* We expect that the vast majority of uses will ignore the particular
* implementation, and the member functons <TT>approximate</tt>
* and <TT>get_appr</tt>.  Such applications will treat <TT>CR</tt> as
* a conventional numerical type, with an interface modelled on
* <TT>java.math.BigInteger</tt>.  No subclasses of <TT>CR</tt>
* will be explicitly mentioned by such a program.
* <P>
* All standard arithmetic operations, as well as a few algebraic
* and transcendal functions are provided.  Constructive reals are
* immutable; thus all of these operations return a new constructive real.
* <P>
* A few uses will require explicit construction of approximation functions.
* The requires the construction of a subclass of <TT>CR</tt> with
* an overridden <TT>approximate</tt> function.  Note that <TT>approximate</tt>
* should only be defined, but never called.  <TT>get_appr</tt>
* provides the same functionality, but adds the caching necessary to obtain
* reasonable performance.
* <P>
* Any operation may throw <TT>com.sgi.math.AbortedError</tt> if the thread in
* which it is executing is interrupted.  (<TT>InterruptedException</tt> cannot
* be used for this purpose, since CR inherits from <TT>Number</tt>.)
* <P>
* Any operation may also throw <TT>com.sgi.math.PrecisionOverflowError</tt>
* If the precision request generated during any subcalculation overflows
* a 28-bit integer.  (This should be extremely unlikely, except as an
* outcome of a division by zero, or other erroneous computation.)
* 
*/
public abstract class CR extends Number {
    // CR is the basic representation of a number.
    // Abstractly this is a function for computing an approximation
    // plus the current best approximation.
    // We could do without the latter, but that would
    // be atrociously slow.

    // First some frequently used constants, so we don't have to
    // recompute these all over the place.
      static final BigInteger big0 = BigInteger.valueOf(0);
      static final BigInteger big1 = BigInteger.valueOf(1);
      static final BigInteger bigm1 = BigInteger.valueOf(-1);
      static final BigInteger big2 = BigInteger.valueOf(2);
      static final BigInteger big3 = BigInteger.valueOf(3);
      static final BigInteger big6 = BigInteger.valueOf(6);
      static final BigInteger big8 = BigInteger.valueOf(8);
      static final BigInteger big10 = BigInteger.valueOf(10);

/**
* Setting this to true requests that  all computations be aborted by
* throwing AbortedError.  Must be rest to false before any further
* computation.  Ideally Thread.interrupt() should be used instead, but
* that doesn't appear to be consistently supported by browser VMs.
*/
public volatile static boolean please_stop = false;

/**
* Must be defined in subclasses of <TT>CR</tt>.
* Most users can ignore the existence of this method, and will
* not ever need to define a <TT>CR</tt> subclass.
* Returns value / base ** prec rounded to an integer.
* The error in the result is strictly < 1.
* Informally, approximate(n) gives a scaled approximation
* accurate to 2**n.
* Implementations may safely assume that precision is
* at least a factor of 8 away from overflow.
*/
      protected abstract BigInteger approximate(int precision);
      transient int min_prec;
	// The smallest precision value with which the above
	// has been called.
      transient BigInteger max_appr;
	// The scaled approximation corresponding to min_prec.
      transient boolean appr_valid = false;
	// min_prec and max_val are valid.

    // Helper functions
      static int bound_log2(int n) {
	int abs_n = Math.abs(n);
	return (int)Math.ceil(Math.log((double)(abs_n + 1))/Math.log(2.0));
      }
      // Check that a precision is at least a factor of 8 away from
      // overflowng the integer used to hold a precision spec.
      // We generally perform this check early on, and then convince
      // ourselves that none of the operations performed on precisions
      // inside a function can generatean overflow.
      static void check_prec(int n) {
	int high = n >> 28;
	// if n is not in danger of overflowing, then the 4 high order
	// bits should be identical.  Thus high is either 0 or -1.
	// The rest of this is to test for either of those in a way
	// that should be as cheap as possible.
	int high_shifted = n >> 29;
	if (0 != (high ^ high_shifted)) {
	    throw new PrecisionOverflowError();
	}
      }

/**
* The constructive real number corresponding to a
* <TT>BigInteger</tt>.
*/
      public static CR valueOf(BigInteger n) {
	return new int_CR(n);
      }

/**
* The constructive real number corresponding to a
* Java <TT>int</tt>.
*/ 
      public static CR valueOf(int n) {
	return valueOf(BigInteger.valueOf(n));
      }

/**
* The constructive real number corresponding to a
* Java <TT>long</tt>.
*/ 
      public static CR valueOf(long n) {
	return valueOf(BigInteger.valueOf(n));
      }

/**
* The constructive real number corresponding to a
* Java <TT>double</tt>.
* The result is undefined if argument is infinite or NaN.
*/ 
      public static CR valueOf(double n) {
	if (Double.isNaN(n)) throw new ArithmeticException();
	if (Double.isInfinite(n)) throw new ArithmeticException();
	boolean negative = (n < 0.0);
	long bits = Double.doubleToLongBits(Math.abs(n));
	long mantissa = (bits & 0xfffffffffffffL);
	int biased_exp = (int)(bits >> 52);
	int exp = biased_exp - 1075;
	if (biased_exp != 0) {
	    mantissa += (1L << 52);
	} else {
	    mantissa <<= 1;
	}
	CR result = valueOf(mantissa).shiftLeft(exp);
	if (negative) result = result.negate();
	return result;
      }

/**
* The constructive real number corresponding to a
* Java <TT>float</tt>.
* The result is undefined if argument is infinite or NaN.
*/ 
      public static CR valueOf(float n) {
	return valueOf((double) n);
      }

      static CR one = valueOf(1);

    // Multiply k by 2**n.
      static BigInteger shift(BigInteger k, int n) {
	if (n == 0) return k;
	if (n < 0) return k.shiftRight(-n);
	return k.shiftLeft(n);
      }

    // Multiply by 2**n, rounding result
      static BigInteger scale(BigInteger k, int n) {
	if (n >= 0) {
	    return k.shiftLeft(n);
	} else {
	    BigInteger adj_k = shift(k, n+1).add(big1);
            return adj_k.shiftRight(1);
	}
      }

    // Identical to approximate(), but maintain and update cache.
/**
* Returns value / 2 ** prec rounded to an integer.
* The error in the result is strictly < 1.
* Produces the same answer as <TT>approximate</tt>, but uses and
* maintains a cached approximation.
* Normally not overridden, and called only from <TT>approximate</tt>
* methods in subclasses.  Not needed if the provided operations
* on constructive reals suffice.
*/ 
      public BigInteger get_appr(int precision) {
	check_prec(precision);
	if (appr_valid && precision >= min_prec) {
	    return scale(max_appr, min_prec - precision);
	} else {
	    BigInteger result = approximate(precision);
	    min_prec = precision;
	    max_appr = result;
	    appr_valid = true;
	    return result;   
	}
      }

    // Return the position of the msd.
    // If x.msd() == n then
    // 2**(n-1) < abs(x) < 2**(n+1) 
    // This initial version assumes that max_appr is valid
    // and sufficiently removed from zero
    // that the msd is determined.
      int known_msd() {
	int first_digit;
        int length;
        if (max_appr.signum() >= 0) {
            length = max_appr.bitLength();
        } else {
            length = max_appr.negate().bitLength();
        }
        first_digit = min_prec + length - 1;
        return first_digit;
      }
	
    // This version may return Integer.MIN_VALUE if the correct
    // answer is < n.
      int msd(int n) {
	if (!appr_valid ||
		max_appr.compareTo(big1) <= 0
		&& max_appr.compareTo(bigm1) >= 0) {
	    get_appr(n - 1);
	    if (max_appr.abs().compareTo(big1) <= 0) {
		// msd could still be arbitrarily far to the right.
		return Integer.MIN_VALUE;
	    }
	}
	return known_msd();
      }


    // Functionally equivalent, but iteratively evaluates to higher
    // precision.
      int iter_msd(int n)
      {
	int prec = 0;

     	for (;prec > n + 30; prec = (prec * 3)/2 - 16) {
	    int msd = msd(prec);
	    if (msd != Integer.MIN_VALUE) return msd;
	    check_prec(prec);
	    if (Thread.interrupted() || please_stop) throw new AbortedError();
	}
        return msd(n);
      }

    // This version returns a correct answer eventually, except
    // that it loops forever (or throws an exception when the
    // requested precision overflows) if this constructive real is zero.
      int msd() {
	  return iter_msd(Integer.MIN_VALUE);
      }

    // A helper function for toString.
    // Generate a String containing n zeroes.
      private static String zeroes(int n) {
	char[] a = new char[n];
	for (int i = 0; i < n; ++i) {
	    a[i] = '0';
	}
        return new String(a);
      }    

    // Natural log of 2.  Needed for some prescaling below.
    // ln(2) = 7ln(10/9) - 2ln(25/24) + 3ln(81/80)
	CR simple_ln() {
	    return new prescaled_ln_CR(this.subtract(one));
	}
	static CR ten_ninths = valueOf(10).divide(valueOf(9));
	static CR twentyfive_twentyfourths = valueOf(25).divide(valueOf(24));
	static CR eightyone_eightyeths = valueOf(81).divide(valueOf(80));
	static CR ln2_1 = valueOf(7).multiply(ten_ninths.simple_ln());
	static CR ln2_2 =
		valueOf(2).multiply(twentyfive_twentyfourths.simple_ln());
	static CR ln2_3 = valueOf(3).multiply(eightyone_eightyeths.simple_ln());
	static CR ln2 = ln2_1.subtract(ln2_2).add(ln2_3);

    // Atan of integer reciprocal.  Used for PI.  Could perhaps
    // be made public.
	static CR atan_reciprocal(int n) {
	    return new integral_atan_CR(n);
	}
    // Other constants used for PI computation.
	static CR four = valueOf(4);

  // Public operations.
/**
* Return 0 if x = y to within the indicated tolerance,
* -1 if x < y, and +1 if x > y.  If x and y are indeed
* equal, it is guaranteed that 0 will be returned.  If
* they differ by less than the tolerance, anything
* may happen.  The tolerance allowed is
* the maximum of (abs(this)+abs(x))*(2**r) and 2**a
* 	@param x 	The other constructive real
*	@param r	Relative tolerance in bits
*	@param a	Absolute tolerance in bits
*/
      public int compareTo(CR x, int r, int a) {
	int this_msd = iter_msd(a);
	int x_msd = x.iter_msd(this_msd > a? this_msd : a);
	int max_msd = (x_msd > this_msd? x_msd : this_msd);
 	int rel = max_msd + r;
	    // This can't approach overflow, since r and a are
	    // effectively divided by 2, and msds are checked.
	int abs_prec = (rel > a? rel : a);
	return compareTo(x, abs_prec);
      }

/**
* Approximate comparison with only an absolute tolerance.
* Identical to the three argument version, but without a relative
* tolerance.
* Result is 0 if both constructive reals are equal, indeterminate
* if they differ by less than 2**a.
*
* 	@param x	The other constructive real
*	@param a	Absolute tolerance in bits
*/
      public int compareTo(CR x, int a) {
	int needed_prec = a - 1;
	BigInteger this_appr = get_appr(needed_prec);
	BigInteger x_appr = x.get_appr(needed_prec);
	int comp1 = this_appr.compareTo(x_appr.add(big1));
	if (comp1 > 0) return 1;
	int comp2 = this_appr.compareTo(x_appr.subtract(big1));
	if (comp2 < 0) return -1;
	return 0;
      }

/**
* Should be called only if <TT>x != y</tt>.
* Return -1 if <TT>this < x</tt>, or +1 if <TT>this > x</tt>.
* If <TT>this == x</tt>, this will not terminate correctly; typically it
* will run until it exhausts memory.
* If the two constructive reals may be equal, the two or 3 argument
* version of compareTo should be used.
*/
      public int compareTo(CR x) {
	for (int a = -20; ; a *= 2) {
	    check_prec(a);
	    int result = compareTo(x, a);
	    if (0 != result) return result;
	}
      }

/**
* Equivalent to <TT>compareTo(CR.valueOf(0), a)</tt>
*/
      public int signum(int a) {
	if (appr_valid) {
	    int quick_try = max_appr.signum();
	    if (0 != quick_try) return quick_try;
	}
	int needed_prec = a - 1;
        BigInteger this_appr = get_appr(needed_prec);
	return this_appr.signum();
      }

/**
* Should be called only if <TT>x != 0</tt>.
* Return -1 if negative, +1 if positive.
* In the 0 case, this will not terminate correctly; typically it
* will run until it exhausts memory.
* If the two constructive reals may be equal, the one or two argument
* version of signum should be used.
*/
      public int signum() {
	for (int a = -20; ; a *= 2) {
	    check_prec(a);
	    int result = signum(a);
	    if (0 != result) return result;
	}
      }

/**
* Return the constructive real number corresponding to the given
* textual representation and radix.
*
*	@param s	[-] digit* [. digit*]
*	@param radix
*/

      public static CR valueOf(String s, int radix)
	     throws NumberFormatException {
	  int len = s.length(); 
	  int start_pos = 0, point_pos;
	  String fraction;
	  while (s.charAt(start_pos) == ' ') ++start_pos;
	  while (s.charAt(len - 1) == ' ') --len;
	  point_pos = s.indexOf('.', start_pos);
	  if (point_pos == -1) {
	      point_pos = len;
	      fraction = "0";
	  } else {
	      fraction = s.substring(point_pos + 1, len);
	  }
	  String whole = s.substring(start_pos, point_pos);
	  BigInteger scaled_result = new BigInteger(whole + fraction, radix);
	  BigInteger divisor = BigInteger.valueOf(radix).pow(fraction.length());
	  return CR.valueOf(scaled_result).divide(CR.valueOf(divisor));
      }
	   
/**
* Return a textual representation accurate to <TT>n</tt> places
* to the right of the decimal point.  <TT>n</tt> must be nonnegative.
*
*	@param	n	Number of digits included to the right of decimal point
*	@param  radix	Base ( >= 2, <= 16) for the resulting representation. 
*/
      public String toString(int n, int radix) {
	  CR scaled_CR;
	  if (16 == radix) {
	    scaled_CR = shiftLeft(4*n);
	  } else {
	    BigInteger scale_factor = BigInteger.valueOf(radix).pow(n);
	    scaled_CR = multiply(new int_CR(scale_factor));
	  }
	  BigInteger scaled_int = scaled_CR.get_appr(0);
	  String scaled_string = scaled_int.abs().toString(radix);
	  String result;
	  if (0 == n) {
	      result = scaled_string;
	  } else {
	      int len = scaled_string.length();
	      if (len <= n) {
		// Add sufficient leading zeroes
		  String z = zeroes(n + 1 - len);
		  scaled_string = z + scaled_string;
		  len = n + 1;
	      }
	      String whole = scaled_string.substring(0, len - n);
	      String fraction = scaled_string.substring(len - n);
	      result = whole + "." + fraction;
	  }
	  if (scaled_int.signum() < 0) {
	      result = "-" + result;
	  }
    	  return result;
      }


/**
* Equivalent to <TT>toString(n,10)</tt>
*
*	@param	n	Number of digits included to the right of decimal point
*/
    public String toString(int n) {
	return toString(n, 10);
    }

/**
* Equivalent to <TT>toString(10, 10)</tt>
*/
    public String toString() {
	return toString(10);
    }

/**
* Return a BigInteger which differs by less than one from the
* constructive real.
*/
    public BigInteger BigIntegerValue() {
 	return get_appr(0);
    }

/**
* Return an int which differs by less than one from the
* constructive real.  Behavior on overflow is undefined.
*/
    public int intValue() {
	return BigIntegerValue().intValue();
    }

/**
* Return a long which differs by less than one from the
* constructive real.  Behavior on overflow is undefined.
*/
    public long longValue() {
	return BigIntegerValue().longValue();
    }

/**
* Return a double which differs by less than one in the least
* represented bit from the constructive real.
*/
    public double doubleValue() {
	int my_msd = iter_msd(-1080 /* slightly > exp. range */);
	if (Integer.MIN_VALUE == my_msd) return 0.0;
	int needed_prec = my_msd - 60;
	double scaled_int = get_appr(needed_prec).doubleValue();
	boolean may_underflow = (needed_prec < -1000);
	long scaled_int_rep = Double.doubleToLongBits(scaled_int);
	long exp_adj = may_underflow? needed_prec + 96 : needed_prec;
	long orig_exp = (scaled_int_rep >> 52) & 0x7ff;
        if (((orig_exp + exp_adj) & ~0x7ff) != 0) {
	    // overflow
	    if (scaled_int < 0.0) {
		return Double.NEGATIVE_INFINITY;
	    } else {
		return Double.POSITIVE_INFINITY;
	    }
	}
	scaled_int_rep += exp_adj << 52;
	double result = Double.longBitsToDouble(scaled_int_rep);
	if (may_underflow) {
	    double two48 = (double)(1 << 48);
	    return result/two48/two48;
	} else {
	    return result;
	}
    }

/**
* Return a float which differs by less than one in the least
* represented bit from the constructive real.
*/
    public float floatValue() {
	return (float)doubleValue();
    }

/**
* Add two constructive reals.
*/
    public CR add(CR x) {
        return new add_CR(this, x);
    }

/**
* Multiply a constructive real by 2**n.
* @param n	shift count, may be negative
*/
    public CR shiftLeft(int n) {
	check_prec(n);
	return new shifted_CR(this, n);
    }

/**
* Multiply a constructive real by 2**(-n).
* @param n	shift count, may be negative
*/
    public CR shiftRight(int n) {
	check_prec(n);
	return new shifted_CR(this, -n);
    }

/**
* The additive inverse of a constructive real
*/
    public CR negate() {
        return new neg_CR(this);
    }

/**
* The difference between two constructive reals
*/
    public CR subtract(CR x) {
        return new add_CR(this, x.negate());
    }

/**
* The product of two constructive reals
*/
    public CR multiply(CR x) {
        return new mult_CR(this, x);
    }

/**
* The multiplicative inverse of a constructive real.
* <TT>x.inverse()</tt> is equivalent to <TT>CR.valueOf(1).divide(x)</tt>.
*/
    public CR inverse() {
        return new inv_CR(this);
    }

/**
* The quotient of two constructive reals.
*/
    public CR divide(CR x) {
        return new mult_CR(this, x.inverse());
    }

/**
* The real number <TT>x</tt> if <TT>this</tt> < 0, or <TT>y</tt> otherwise.
* Requires <TT>x</tt> = <TT>y</tt> if <TT>this</tt> = 0.
* Since comparisons may diverge, this is often
* a useful alternative to conditionals.
*/
    public CR select(CR x, CR y) {
	return new select_CR(this, x, y);
    }

/**
* The maximum of two constructive reals.
*/
    public CR max(CR x) {
	return subtract(x).select(x, this);
    }

/**
* The minimum of two constructive reals.
*/
    public CR min(CR x) {
	return subtract(x).select(this, x);
    }

/**
* The absolute value of a constructive reals.
* Note that this cannot be written as a conditional.
*/
    public CR abs() {
	return select(negate(), this);
    }

/**
* The exponential function, i.e. e**<TT>this</tt>.
*/
    public CR exp() {
      	final int low_prec = -10;
      	BigInteger rough_appr = get_appr(low_prec);
      	if (rough_appr.signum() < 0) return negate().exp().inverse();
      	if (rough_appr.compareTo(big2) > 0) {
	    CR square_root = shiftRight(1).exp();
	    return square_root.multiply(square_root);
      	} else {
     	    return new prescaled_exp_CR(this);
        }
    }

    static CR two = valueOf(2);

/**
* The ratio of a circle's circumference to its diameter.
*/
    public static CR PI = four.multiply(four.multiply(atan_reciprocal(5))
					    .subtract(atan_reciprocal(239)));
	// pi/4 = 4*atan(1/5) - atan(1/239)
    static CR half_pi = PI.shiftRight(1);

/**
* The trigonometric cosine function.
*/
    public CR cos() {
	BigInteger rough_appr = get_appr(-1);
	BigInteger abs_rough_appr = rough_appr.abs();
	if (abs_rough_appr.compareTo(big6) >= 0) {
	    // Subtract multiples of PI
	    BigInteger multiplier = rough_appr.divide(big6);
	    CR adjustment = PI.multiply(CR.valueOf(multiplier));
	    if (multiplier.and(big1).signum() != 0) {
		return subtract(adjustment).cos().negate();
	    } else {
		return subtract(adjustment).cos();
	    }
	} else if (abs_rough_appr.compareTo(big2) >= 0) {
	    // Scale further with double angle formula
	    CR cos_half = shiftRight(1).cos();
	    return cos_half.multiply(cos_half).shiftLeft(1).subtract(one);
	} else {
	    return new prescaled_cos_CR(this);
	}
    }

/**
* The trigonometric sine function.
*/
    public CR sin() {
	return half_pi.subtract(this).cos();
    }

    static final BigInteger low_ln_limit = big8; /* sixteenths, i.e. 1/2 */
    static final BigInteger high_ln_limit =
			BigInteger.valueOf(16 + 8 /* 1.5 */);
    static final BigInteger scaled_4 = 
			BigInteger.valueOf(4*16);

/**
* The natural (base e) logarithm.
*/
    public CR ln() {
	final int low_prec = -4;
	BigInteger rough_appr = get_appr(low_prec); /* In sixteenths */
	if (rough_appr.compareTo(big0) < 0) {
	    throw new ArithmeticException();
	}
	if (rough_appr.compareTo(low_ln_limit) <= 0) {
	    return inverse().ln().negate();
	}
	if (rough_appr.compareTo(high_ln_limit) >= 0) {
	    if (rough_appr.compareTo(scaled_4) <= 0) {
	    	CR quarter = sqrt().sqrt().ln();
	    	return quarter.shiftLeft(2);
	    } else {
		int extra_bits = rough_appr.bitLength() - 3;
		CR scaled_result = shiftRight(extra_bits).ln();
		return scaled_result.add(CR.valueOf(extra_bits).multiply(ln2));
	    }
	}
	return simple_ln();
    }

/**
* The square root of a constructive real.
*/
    public CR sqrt() {
	return new sqrt_CR(this);
    }

}  // end of CR


//
// A specialization of CR for cases in which approximate() calls
// to increase evaluation precision are somewhat expensive.
// If we need to (re)evaluate, we speculatively evaluate to slightly
// higher precision, miminimizing reevaluations.
// Note that this requires any arguments to be evaluated to higher
// precision than absolutely necessary.  It can thus potentially
// result in lots of wasted effort, and should be used judiciously.
// This assumes that the order of magnitude of the number is roughly one.
//
abstract class slow_CR extends CR {
    static int max_prec = -64;
    static int prec_incr = 32;
    public BigInteger get_appr(int precision) {
	check_prec(precision);
	if (appr_valid && precision >= min_prec) {
	    return scale(max_appr, min_prec - precision);
	} else {
	    int eval_prec = (precision >= max_prec? max_prec :
			     (precision - prec_incr + 1) & ~(prec_incr - 1));
	    BigInteger result = approximate(eval_prec);
	    min_prec = eval_prec;
	    max_appr = result;
	    appr_valid = true;
	    return scale(result, eval_prec - precision);   
	}
    }
}


// Representation of an integer constant.  Private.
class int_CR extends CR {
    BigInteger value;
    int_CR(BigInteger n) {
	value = n;
    }
    protected BigInteger approximate(int p) {
	return scale(value, -p) ;
    }
}

// Representation of the sum of 2 constructive reals.  Private.
class add_CR extends CR {
    CR op1;
    CR op2;
    add_CR(CR x, CR y) {
	op1 = x;
	op2 = y;
    }
    protected BigInteger approximate(int p) {
	// Args need to be evaluated so that each error is < 1/4 ulp.
	// Rounding error from the cale call is <= 1/2 ulp, so that
	// final error is < 1 ulp.
	return scale(op1.get_appr(p-2).add(op2.get_appr(p-2)), -2);
    }
}

// Representation of a CR multiplied by 2**n
class shifted_CR extends CR {
    CR op;
    int count;
    shifted_CR(CR x, int n) {
	op = x;
	count = n;
    }
    protected BigInteger approximate(int p) {
	return op.get_appr(p - count);
    }
}

// Representation of the negation of a constructive real.  Private.
class neg_CR extends CR {
    CR op;
    neg_CR(CR x) {
	op = x;
    }
    protected BigInteger approximate(int p) {
	return op.get_appr(p).negate();
    }
}

// Representation of:
//	op1	if selector < 0
//	op2	if selector >= 0
// Assumes x = y if s = 0 
class select_CR extends CR {
    CR selector;
    int selector_sign;
    CR op1;
    CR op2;
    select_CR(CR s, CR x, CR y) {
	selector = s;
	int selector_sign = selector.get_appr(-20).signum();
	op1 = x;
	op2 = y;
    }
    protected BigInteger approximate(int p) {
	if (selector_sign < 0) return op1.get_appr(p);
	if (selector_sign > 0) return op2.get_appr(p);
	BigInteger op1_appr = op1.get_appr(p-1);
	BigInteger op2_appr = op2.get_appr(p-1);
	BigInteger diff = op1_appr.subtract(op2_appr).abs();
	if (diff.compareTo(big1) <= 0) {
	    // close enough; use either
	    return scale(op1_appr, -1);
	}
	// op1 and op2 are different; selector != 0;
	// safe to get sign of selector.
	if (selector.signum() < 0) {
	    selector_sign = -1;
	    return scale(op1_appr, -1);
	} else {
	    selector_sign = 1;
	    return scale(op2_appr, -1);
	}
    }
}

// Representation of the product of 2 constructive reals. Private.
class mult_CR extends CR {
    CR op1;
    CR op2;
    mult_CR(CR x, CR y) {
	op1 = x;
	op2 = y;
    }
    protected BigInteger approximate(int p) {
	int half_prec = (p >> 1) - 1;
    	int msd_op1 = op1.msd(half_prec);
    	int msd_op2;

	if (msd_op1 == Integer.MIN_VALUE) {
	    msd_op2 = op2.msd(half_prec);
	    if (msd_op2 == Integer.MIN_VALUE) {
		// Product is small enough that zero will do as an
		// approximation.
		return big0;
	    } else {
		// Swap them, so the larger operand (in absolute value)
		// is first.
		CR tmp;
		tmp = op1;
		op1 = op2;
		op2 = tmp;
		msd_op1 = msd_op2;
	    }
	} 
	// msd_op1 is valid at this point.
        int prec2 = p - msd_op1 - 3;  	// Precision needed for op2.
		// The appr. error is multiplied by at most
		// 2 ** (msd_op1 + 1)
		// Thus each approximation contributes 1/4 ulp
		// to the rounding error, and the final rounding adds
		// another 1/2 ulp.
	BigInteger appr2 = op2.get_appr(prec2);
        if (appr2.signum() == 0) return big0;
	msd_op2 = op2.known_msd();
	int prec1 = p - msd_op2 - 3;	// Precision needed for op1.
	BigInteger appr1 = op1.get_appr(prec1);
	int scale_digits =  prec1 + prec2 - p;
	return scale(appr1.multiply(appr2), scale_digits);
    }
}

// Representation of the multiplicative invers of a constructive
// real.  Private.  Should use Newton iteration to refine estimates.
class inv_CR extends CR {
    CR op;
    inv_CR(CR x) { op = x; }
    protected BigInteger approximate(int p) {
	int msd = op.msd();
	int inv_msd = 1 - msd;
	int digits_needed = inv_msd - p + 3;
                                // Number of SIGNIFICANT digits needed for 
                                // argument, excl. msd position, which may 
                                // be fictitious, since msd routine can be 
                                // off by 1.  Roughly 1 extra digit is     
                                // needed since the relative error is the  
                                // same in the argument and result, but    
                                // this isn't quite the same as the number 
                                // of significant digits.  Another digit   
                                // is needed to compensate for slop in the 
                                // calculation.
				// One further bit is required, since the
				// final rounding introduces a 0.5 ulp
				// error.
	int prec_needed = msd - digits_needed;
	int log_scale_factor = -p - prec_needed;
	if (log_scale_factor < 0) return big0;
	BigInteger dividend = big1.shiftLeft(log_scale_factor);
	BigInteger scaled_divisor = op.get_appr(prec_needed);
	BigInteger abs_scaled_divisor = scaled_divisor.abs();
	BigInteger adj_dividend = dividend.add(
					abs_scaled_divisor.shiftRight(1));
		// Adjustment so that final result is rounded.
	BigInteger result = adj_dividend.divide(abs_scaled_divisor);
	if (scaled_divisor.signum() < 0) {
	  return result.negate();
	} else {
	  return result;
	}
    }
}


// Representation of the exponential of a constructive real.  Private.
// Uses a Taylor series expansion.  Assumes x < 1/2.
// Note: this is known to be a bad algorithm for
// floating point.  Unfortunately, other alternatives
// appear to require precomputed information.
class prescaled_exp_CR extends CR {
    CR op;
    prescaled_exp_CR(CR x) { op = x; }
    protected BigInteger approximate(int p) {
	if (p >= 1) return big0;
	int iterations_needed = -p/2 + 2;  // conservative estimate > 0.
	  //  Claim: each intermediate term is accurate
	  //  to 2*2^calc_precision.
	  //  Total rounding error in series computation is
	  //  2*iterations_needed*2^calc_precision,
	  //  exclusive of error in op.
	int calc_precision = p - bound_log2(2*iterations_needed)
			       - 4; // for error in op, truncation.
	int op_prec = p - 3;
	BigInteger op_appr = op.get_appr(op_prec);
	  // Error in argument results in error of < 3/8 ulp.
	  // Sum of term eval. rounding error is < 1/16 ulp.
	  // Series truncation error < 1/16 ulp.
	  // Final rounding error is <= 1/2 ulp.
	  // Thus final error is < 1 ulp.
 	BigInteger scaled_1 = big1.shiftLeft(-calc_precision);
	BigInteger current_term = scaled_1;
	BigInteger current_sum = scaled_1;
	int n = 0;
	BigInteger max_trunc_error =
		big1.shiftLeft(p - 4 - calc_precision);
	while (current_term.abs().compareTo(max_trunc_error) >= 0) {
	  if (Thread.interrupted() || please_stop) throw new AbortedError();
	  n += 1;
	  /* current_term = current_term * op / n */
	  current_term = scale(current_term.multiply(op_appr), op_prec);
	  current_term = current_term.divide(BigInteger.valueOf(n));
	  current_sum = current_sum.add(current_term);
	}
    	return scale(current_sum, calc_precision - p);
    }
}

// Representation of the cosine of a constructive real.  Private.
// Uses a Taylor series expansion.  Assumes |x| < 1.
class prescaled_cos_CR extends slow_CR {
    CR op;
    prescaled_cos_CR(CR x) {
	op = x;
    }
    protected BigInteger approximate(int p) {
	if (p >= 1) return big0;
	int iterations_needed = -p/2 + 4;  // conservative estimate > 0.
	  //  Claim: each intermediate term is accurate
	  //  to 2*2^calc_precision.
	  //  Total rounding error in series computation is
	  //  2*iterations_needed*2^calc_precision,
	  //  exclusive of error in op.
	int calc_precision = p - bound_log2(2*iterations_needed)
			       - 4; // for error in op, truncation.
	int op_prec = p - 2;
	BigInteger op_appr = op.get_appr(op_prec);
	  // Error in argument results in error of < 1/4 ulp.
	  // Cumulative arithmetic rounding error is < 1/16 ulp.
	  // Series truncation error < 1/16 ulp.
	  // Final rounding error is <= 1/2 ulp.
	  // Thus final error is < 1 ulp.
	BigInteger current_term;
	int n;
	BigInteger max_trunc_error =
		big1.shiftLeft(p - 4 - calc_precision);
	n = 0;
	current_term = big1.shiftLeft(-calc_precision);
	BigInteger current_sum = current_term;
	while (current_term.abs().compareTo(max_trunc_error) >= 0) {
	  if (Thread.interrupted() || please_stop) throw new AbortedError();
	  n += 2;
	  /* current_term = - current_term * op * op / n * (n - 1)   */
	  current_term = scale(current_term.multiply(op_appr), op_prec);
	  current_term = scale(current_term.multiply(op_appr), op_prec);
	  BigInteger divisor = BigInteger.valueOf(-n)
				  .multiply(BigInteger.valueOf(n-1));
	  current_term = current_term.divide(divisor);
	  current_sum = current_sum.add(current_term);
	}
    	return scale(current_sum, calc_precision - p);
    }
}

// The constructive real atan(1/n), where n is a small integer
// > base.
// This gives a simple and moderately fast way to compute PI.
class integral_atan_CR extends slow_CR {
    int op;
    integral_atan_CR(int x) { op = x; }
    protected BigInteger approximate(int p) {
	if (p >= 1) return big0;
	int iterations_needed = -p/2 + 2;  // conservative estimate > 0.
	  //  Claim: each intermediate term is accurate
	  //  to 2*base^calc_precision.
	  //  Total rounding error in series computation is
	  //  2*iterations_needed*base^calc_precision,
	  //  exclusive of error in op.
	int calc_precision = p - bound_log2(2*iterations_needed)
			       - 2; // for error in op, truncation.
	  // Error in argument results in error of < 3/8 ulp.
	  // Cumulative arithmetic rounding error is < 1/4 ulp.
	  // Series truncation error < 1/4 ulp.
	  // Final rounding error is <= 1/2 ulp.
	  // Thus final error is < 1 ulp.
 	BigInteger scaled_1 = big1.shiftLeft(-calc_precision);
        BigInteger big_op = BigInteger.valueOf(op);
        BigInteger big_op_squared = BigInteger.valueOf(op*op);
	BigInteger op_inverse = scaled_1.divide(big_op);
	BigInteger current_power = op_inverse;
	BigInteger current_term = op_inverse;
	BigInteger current_sum = op_inverse;
	int current_sign = 1;
	int n = 1;
	BigInteger max_trunc_error =
		big1.shiftLeft(p - 2 - calc_precision);
	while (current_term.abs().compareTo(max_trunc_error) >= 0) {
	  if (Thread.interrupted() || please_stop) throw new AbortedError();
	  n += 2;
	  current_power = current_power.divide(big_op_squared);
	  current_sign = -current_sign;
	  current_term =
	    current_power.divide(BigInteger.valueOf(current_sign*n));
          current_sum = current_sum.add(current_term);
	}
    	return scale(current_sum, calc_precision - p);
    }
}

// Representation for ln(1 + op)
class prescaled_ln_CR extends slow_CR {
    CR op;
    prescaled_ln_CR(CR x) { op = x; }
    // Compute an approximation of ln(1+x) to precision  
    // prec. This assumes |x| < 1/2.                    
    // It uses a Taylor series expansion.                
    // Unfortunately there appears to be no way to take  
    // advantage of old information.                     
    // Note: this is known to be a bad algorithm for     
    // floating point.  Unfortunately, other alternatives
    // appear to require precomputed tabular information.            
    protected BigInteger approximate(int p) {
	if (p >= 0) return big0;
	int iterations_needed = -p;  // conservative estimate > 0.
	  //  Claim: each intermediate term is accurate
	  //  to 2*2^calc_precision.  Total error is
	  //  2*iterations_needed*2^calc_precision
	  //  exclusive of error in op.
	int calc_precision = p - bound_log2(2*iterations_needed)
			       - 4; // for error in op, truncation.
	int op_prec = p - 3;
	BigInteger op_appr = op.get_appr(op_prec);
	  // Error analysis as for exponential.
 	BigInteger scaled_1 = big1.shiftLeft(-calc_precision);
	BigInteger x_nth = scale(op_appr, op_prec - calc_precision);
   	BigInteger current_term = x_nth;  // x**n
	BigInteger current_sum = current_term;
	int n = 1;
	int current_sign = 1;	// (-1)^(n-1)
	BigInteger max_trunc_error =
		big1.shiftLeft(p - 4 - calc_precision);
	while (current_term.abs().compareTo(max_trunc_error) >= 0) {
	  if (Thread.interrupted() || please_stop) throw new AbortedError();
	  n += 1;
          current_sign = -current_sign;
	  x_nth = scale(x_nth.multiply(op_appr), op_prec);
	  current_term = x_nth.divide(BigInteger.valueOf(n * current_sign));
				// x**n / (n * (-1)**(n-1))
	  current_sum = current_sum.add(current_term);
	}
    	return scale(current_sum, calc_precision - p);
    }
}

class sqrt_CR extends CR {
    CR op;
    sqrt_CR(CR x) { op = x; }
    final int fp_prec = 50;	// Conservative estimate of number of
				// significant bits in double precision
				// computation.
    final int fp_op_prec = 60;
    protected BigInteger approximate(int p) {
	int max_prec_needed = 2*p - 1;
	int msd = op.msd(max_prec_needed);
	if (msd <= max_prec_needed) return big0; 
	int result_msd = msd/2;			// +- 1
        int result_digits = result_msd - p; 	// +- 2
	if (result_digits > fp_prec) {
	  // Compute less precise approximation and use a Newton iter.
	    int appr_digits = result_digits/2 + 6;
		// This should be conservative.  Is fewer enough?
	    int appr_prec = result_msd - appr_digits;
	    BigInteger last_appr = get_appr(appr_prec);
	    int prod_prec = 2*appr_prec;
	    BigInteger op_appr = op.get_appr(prod_prec);
		// Slightly fewer might be enough;
	    // Compute (last_appr * last_appr + op_appr)/(last_appr/2)
	    // while adjusting the scaling to make everything work
	    BigInteger prod_prec_scaled_numerator =
		last_appr.multiply(last_appr).add(op_appr);
	    BigInteger scaled_numerator = 
		scale(prod_prec_scaled_numerator, appr_prec - p);
	    BigInteger shifted_result = scaled_numerator.divide(last_appr);
	    return shifted_result.add(big1).shiftRight(1);
	} else {
	  // Use a double precision floating point approximation.
	    // Make sure all precisions are even
	    int op_prec = (msd - fp_op_prec) & ~1;
	    int working_prec = op_prec - fp_op_prec;
	    BigInteger scaled_bi_appr = op.get_appr(op_prec)
				        .shiftLeft(fp_op_prec);
	    double scaled_appr = scaled_bi_appr.doubleValue();
	    if (scaled_appr < 0.0) throw new ArithmeticException();
	    double scaled_fp_sqrt = Math.sqrt(scaled_appr);
	    BigInteger scaled_sqrt = BigInteger.valueOf((long)scaled_fp_sqrt);
	    int shift_count = working_prec/2 - p;
	    return shift(scaled_sqrt, shift_count);
	}
    }
}
