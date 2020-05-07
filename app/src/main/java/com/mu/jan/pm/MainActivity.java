package com.mu.jan.pm;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.mu.jan.pm.db.FavoriteMovie;
import com.mu.jan.pm.model.MoviesClass;
import com.mu.jan.pm.utils.AppConstants;
import com.mu.jan.pm.utils.JsonUtils;
import com.mu.jan.pm.utils.NetworkUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements MovieAdapter.ListItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SORT_POPULAR = "popular";
    private static String currentSort = SORT_POPULAR;

    private ArrayList<MoviesClass> movieList;

    private RecyclerView mMovieRecyclerView;
    private MovieAdapter mMovieAdapter;

    private List<FavoriteMovie> favMovs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //recyclerview
        mMovieRecyclerView = (RecyclerView) findViewById(R.id.rv_main);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        mMovieRecyclerView.setLayoutManager(layoutManager);
        mMovieRecyclerView.setHasFixedSize(true);

        mMovieAdapter = new MovieAdapter(movieList, this, this);
        mMovieRecyclerView.setAdapter(mMovieAdapter);

        favMovs = new ArrayList<FavoriteMovie>();

        setTitle(getString(R.string.app_name) + " - Popular");

        setupViewModel();
    }

    private void loadMovies() {
        makeMovieSearchQuery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort_popular && !currentSort.equals(SORT_POPULAR)) {
            ClearMovieItemList();
            currentSort = SORT_POPULAR;
            setTitle(getString(R.string.app_name) + " - Popular");
            loadMovies();
            return true;
        }
        if (id == R.id.action_sort_top_rated && !currentSort.equals(AppConstants.SORT_TOP_RATED)) {
            ClearMovieItemList();
            currentSort = AppConstants.SORT_TOP_RATED;
            setTitle(getString(R.string.app_name) + " - Top rated");
            loadMovies();
            return true;
        }
        if (id == R.id.action_sort_favorite && !currentSort.equals(AppConstants.SORT_FAVORITE)) {
            ClearMovieItemList();
            currentSort = AppConstants.SORT_FAVORITE;
            setTitle(getString(R.string.app_name) + " - Favorite");
            loadMovies();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ClearMovieItemList() {
        if (movieList != null) {
            movieList.clear();
        } else {
            movieList = new ArrayList<MoviesClass>();
        }
    }

    private void makeMovieSearchQuery() {
        if (currentSort.equals(AppConstants.SORT_FAVORITE)) {
            ClearMovieItemList();
            for (int i = 0; i< favMovs.size(); i++) {
                MoviesClass mov = new MoviesClass(
                        String.valueOf(favMovs.get(i).getId()),
                        favMovs.get(i).getTitle(),
                        favMovs.get(i).getReleaseDate(),
                        favMovs.get(i).getVote(),
                        favMovs.get(i).getPopularity(),
                        favMovs.get(i).getSynopsis(),
                        favMovs.get(i).getImage(),
                        favMovs.get(i).getBackdrop()
                );
                movieList.add( mov );
            }
            mMovieAdapter.setMovieData(movieList);
        } else {
            String movieQuery = currentSort;
            URL movieSearchUrl = NetworkUtils.buildUrl(movieQuery,getResources().getString(R.string.api_key));
            new MoviesQueryTask().execute(movieSearchUrl);
        }
    }

    // AsyncTask to perform query
    public class MoviesQueryTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String searchResults = null;
            try {
                searchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return searchResults;
        }

        @Override
        protected void onPostExecute(String searchResults) {
            if (searchResults != null && !searchResults.equals("")) {
                movieList = JsonUtils.parseMoviesJson(searchResults);
                mMovieAdapter.setMovieData(movieList);
            }
        }
    }

    private void setupViewModel() {
        MyViewModel viewModel = ViewModelProviders.of(this).get(MyViewModel.class);
        viewModel.getMovies().observe(this, new Observer<List<FavoriteMovie>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteMovie> favs) {
                if(favs.size()>0) {
                    favMovs.clear();
                    favMovs = favs;
                }
                for (int i=0; i<favMovs.size(); i++) {
                    Log.d(TAG,favMovs.get(i).getTitle());
                }
                loadMovies();
            }
        });
    }

    @Override
    public void OnListItemClick(MoviesClass movieItem) {
        Intent myIntent = new Intent(this, MovieDetails.class);
        myIntent.putExtra("movieItem", movieItem);
        startActivity(myIntent);
    }
}
