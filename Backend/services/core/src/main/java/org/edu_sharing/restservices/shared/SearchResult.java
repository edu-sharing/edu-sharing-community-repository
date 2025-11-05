package org.edu_sharing.restservices.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.Data;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;
import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class SearchResult<T> {

	@JsonProperty(required = true)
	private List<T> nodes = new ArrayList<T>();
	@JsonProperty(required = true)
	private Pagination pagination = null;
	@JsonProperty(required = true)
	private List<Facet> facets = null;
	private List<NodeSearch.Suggest> suggests = null;
	private List<String> ignored;

    /**
     * Applies the given mapping function to each element in the current {@code SearchResult}
     * and returns a new {@code SearchResult} containing the mapped elements.
     *
     * @param <U> the type of elements in the resulting {@code SearchResult}
     * @param mapper the function to apply to each element in the current {@code SearchResult}
     * @return a new {@code SearchResult} containing elements transformed by the provided mapping function
     */
    public <U> SearchResult<U> map(Function<T, U> mapper) {
        return map(mapper, SearchResult::new);
    }

    /**
     * Applies the provided mapping function to each element in the current {@code SearchResult}
     * and returns a new {@code SearchResult} or subclass of it containing the mapped elements.
     *
     * @param <U> the type of elements in the resulting {@code SearchResult}
     * @param <I> the type of the resulting {@code SearchResult} or its subclass
     * @param mapper the function to apply to each element in the current {@code SearchResult}
     * @param supplier a supplier that provides a new instance of the desired {@code SearchResult} or subclass
     * @return a new {@code SearchResult} or subclass containing elements transformed by the provided mapping function
     */
    public <U, I extends SearchResult<U>> I map(Function<T, U> mapper, Supplier<I> supplier) {
        I result = supplier.get();
        result.getNodes().addAll(nodes.stream().map(mapper).toList());
        return result;
    }
}
