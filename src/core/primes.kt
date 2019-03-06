package core

// class primes {
// open class Generator(val value: Int, val igen: Generator) {
// open fun gen (): Pair<Int, Generator> = value to igen
// }
// //typealias Generator = () -> Pair<Int, Generator>
// fun erastosthenes(): () -> Int {
// var pGen = ::prime
// return {
// val (num, nGen) = pGen().gen()
// pGen = ::nGen
// num
// }
// }
//
// //
// //type Generator () -> Pair<Int, Generator>
//
// fun from(a: Int): Generator {
// return Generator(a, from(a + 1))
// }
//
// fun filter(pred: (Int) -> Boolean, gen: Generator): Generator {
// var (num, nGen) = gen.gen()
// if (pred(num)) {
// return Generator(num, filter(pred, nGen))
// }
// return filter(pred, nGen)
// }
//
// //sift returns a fun lacking any multiples of n
// fun sift(n: Int, gen: Generator): Generator {
// return filter( { it%n != 0 }, gen)
// }
//
// fun phelp(gen: Generator): Generator {
// var (num, _) = gen.gen()
// return Generator(num, phelp(sift(num, gen)))
// }
//
// fun prime(): Generator {
// return Generator(2, phelp(sift(2, from(2))))
// }
//
// }