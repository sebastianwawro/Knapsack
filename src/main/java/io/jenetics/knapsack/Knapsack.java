/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */

package io.jenetics.knapsack;

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.internal.util.require;
import io.jenetics.util.ISeq;

import java.io.Serializable;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * <i>Canonical</i> definition of the <i>Knapsack</i> problem. This
 * <i>reference</i> implementation is used for (evolution) performance tests of
 * the GA {@link io.jenetics.engine.Engine}.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version 3.4
 * @since 3.4
 */
public final class Knapsack implements Problem<ISeq<Knapsack.Item>, BitGene, Double> {
	private static int i=0;
	/**
	 * This class represents a knapsack item with the specific <i>size</i> and
	 * <i>value</i>.
	 */
	public static final class Item implements Serializable {
		private static final long serialVersionUID = 1L;

		private final double _size;
		private final double _value;

		private Item(final double size, final double value) {
			_size = require.nonNegative(size);
			_value = require.nonNegative(value);
		}

		/**
		 * Return the item size.
		 *
		 * @return the item size
		 */
		public double getSize() {
			return _size;
		}

		/**
		 * Return the item value.
		 *
		 * @return the item value
		 */
		public double getValue() {
			return _value;
		}

		@Override
		public int hashCode() {
			int hash = 1;
			long bits = Double.doubleToLongBits(_size);
			hash = 31*hash + (int)(bits^(bits >>> 32));

			bits = Double.doubleToLongBits(_value);
			return 31*hash + (int)(bits^(bits >>> 32));
		}

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof Item &&
				Double.compare(_size, ((Item)obj)._size) == 0 &&
				Double.compare(_value, ((Item)obj)._value) == 0;
		}

		@Override
		public String toString() {
			return format("Item[size=%f, value=%f]", _size, _value);
		}

		/**
		 * Return a new knapsack {@code Item} with the given {@code size} and
		 * {@code value}.
		 *
		 * @param size the item size
		 * @param value the item value
		 * @return a new knapsack {@code Item}
		 * @throws IllegalArgumentException if one of the parameters is smaller
		 *         then zero
		 */
		public static Item of(final double size, final double value) {
			return new Item(size, value);
		}

		/**
		 * Create a new <i>random</i> knapsack item for testing purpose.
		 *
		 * @param random the random engine used for creating the knapsack item
		 * @return a new <i>random</i> knapsack item
		 * @throws NullPointerException if the random engine is {@code null}
		 */
		public static Item random(final Random random) {
			return new Item(random.nextDouble()*100, random.nextDouble()*100);
		}

		/**
		 * NEXT FROM TABLE
		 */
		public static Item getFromTable(int index, final double[] itemSizes, final double[] itemValues) {
			return new Item(itemSizes[index], itemValues[index]);
		}

		/**
		 * Return a {@link Collector}, which sums the size and value of knapsack
		 * items.
		 *
		 * @return a knapsack item sum {@link Collector}
		 */
		public static Collector<Item, ?, Item> toSum() {
			return Collector.of(
				() -> new double[2],
				(a, b) -> {a[0] += b._size; a[1] += b._value;},
				(a, b) -> {a[0] += b[0]; a[1] += b[1]; return a;},
				r -> new Item(r[0], r[1])
			);
		}
	}


	private final Codec<ISeq<Item>, BitGene> _codec;
	private final double _knapsackSize;

	/**
	 * Create a new {@code Knapsack} definition with the given
	 *
	 * @param items the basic {@link Set} of knapsack items.
	 * @param knapsackSize the maximal knapsack size
	 * @throws NullPointerException if the {@code items} set is {@code null}
	 */
	public Knapsack(final ISeq<Item> items, final double knapsackSize) {
		_codec = Codecs.ofSubSet(items);
		_knapsackSize = knapsackSize;
	}

	@Override
	public Function<ISeq<Item>, Double> fitness() {
		return items -> {
			final Item sum = items.stream().collect(Item.toSum());
			//return sum._size <= _knapsackSize ? sum._value : 0;
			return (sum._size<=_knapsackSize)?(sum._value*(sum._size/_knapsackSize)):(-sum._size);
	};
	}

	@Override
	public Codec<ISeq<Item>, BitGene> codec() {
		return _codec;
	}

	/**
	 * Factory method for creating <i>same</i> Knapsack problems for testing
	 * purpose.
	 *
	 * @param itemCount the number of knapsack items in the basic set
	 * @param random the random engine used for creating random knapsack items.
	 *        This allows to create reproducible item sets and reproducible
	 *        {@code Knapsack} problems, respectively.
	 * @return a {@code Knapsack} problem object (for testing purpose).
	 */
	public static Knapsack of(final int itemCount, final double[] itemSizes, final double[] itemValues, double backpackSize) {
		//requireNonNull(random);
		i=0;
		return new Knapsack(
			Stream.generate(() -> Item.getFromTable(i++, itemSizes, itemValues))
				.limit(itemCount)
				.collect(ISeq.toISeq()),
			backpackSize
		);
	}

	public static String solveKnapsack(final int itemCount,       //tables size
							final double[] itemSizes,  //table
							final double[] itemValues, //table
							final double backpackSize,
							final int populationSize, //500
							final int tournamentSelectorSampleSize, //5
							final double mutatorProbability, //0.115
							final double crossoverProbability, //0.16
							final int generationsCount, //7
							final int generationsLimit,  //100
							boolean printResults
	) {

		final Knapsack knapsack = Knapsack.of(itemCount, itemSizes, itemValues, backpackSize);

		// Configure and build the evolution engine.
		final Engine<BitGene, Double> engine = Engine.builder(knapsack)
			.populationSize(populationSize)
			.survivorsSelector(new TournamentSelector<>(tournamentSelectorSampleSize))
			//.offspringSelector(new RouletteWheelSelector<>())
			.alterers(
				new Mutator<>(mutatorProbability),
				new SinglePointCrossover<>(crossoverProbability))
			.build();

		// Create evolution statistics consumer.
		final EvolutionStatistics<Double, ?>
			statistics = EvolutionStatistics.ofNumber();

		final Phenotype<BitGene, Double> best = engine.stream()
			// Truncate the evolution stream after 7 "steady"
			// generations.
			//.limit(bySteadyFitness(generationsCount))
			// The evolution will stop after maximal 100
			// generations.
			.limit(generationsLimit)
			// Update the evaluation statistics after
			// each generation
			.peek(statistics)
			// Collect (reduce) the evolution stream to
			// its best phenotype.
			.collect(toBestPhenotype());

		if (printResults) {
			System.out.println(statistics);
			System.out.println(best);
		}
		return best.toString();
	}

}
