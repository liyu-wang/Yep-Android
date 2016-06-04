package catchla.yep.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import catchla.yep.Constants
import catchla.yep.R
import catchla.yep.util.AMapModelUtils
import catchla.yep.util.Utils
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.LocationSource
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.*
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.hannesdorfmann.adapterdelegates.AdapterDelegate
import com.hannesdorfmann.adapterdelegates.ListDelegationAdapter
import java.util.*

/**
 * Created by mariotaku on 16/1/3.
 */
class LocationPickerActivity : ContentActivity(), Constants, LocationListener, LoaderManager.LoaderCallbacks<PoiResult> {
    // Views
    private var mMapView: MapView? = null

    private var mLocationManager: LocationManager? = null
    private var mOnLocationChangedListener: LocationSource.OnLocationChangedListener? = null
    private val mLocationSource = object : LocationSource {
        override fun activate(listener: LocationSource.OnLocationChangedListener) {
            mOnLocationChangedListener = listener
        }

        override fun deactivate() {
            mOnLocationChangedListener = null
        }
    }
    private var mMap: AMap? = null
    private var mLoaderInitialized: Boolean = false
    private var mPlacesList: RecyclerView? = null
    private var mAdapter: LocationAdapter? = null
    private var mMarker: Marker? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_location_picker, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setContentView(R.layout.activity_location_picker)
        mMapView!!.onCreate(savedInstanceState)
        mAdapter = LocationAdapter(this)
        mPlacesList!!.adapter = mAdapter
        mPlacesList!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        setupMap()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.use_location -> {
                val myLocation = mMap!!.myLocation
                val data = Intent()
                if (mMarker != null) {
                    val location = Location("")
                    val position = mMarker!!.position
                    location.latitude = position.latitude
                    location.longitude = position.longitude
                    data.putExtra(Constants.EXTRA_LOCATION, location)
                    data.putExtra(Constants.EXTRA_NAME, mMarker!!.title)
                } else if (myLocation != null) {
                    data.putExtra(Constants.EXTRA_LOCATION, myLocation)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return true
                }
                setResult(Activity.RESULT_OK, data)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupMap() {
        mMap = mMapView!!.map
        val style = MyLocationStyle()
        style.radiusFillColor(0x200079ff)
        style.strokeColor(Color.TRANSPARENT)
        style.strokeWidth(0f)
        val bitmap = Utils.getMarkerBitmap(this)
        style.myLocationIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
        style.anchor(.5f, .5f)
        mMap!!.setMyLocationStyle(style)
        mMap!!.setLocationSource(mLocationSource)
        mMap!!.setOnMapClickListener { latLng -> showMarker(latLng, "Pin on map", false) }
        mMap!!.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: CameraPosition) {

            }

            override fun onCameraChangeFinish(position: CameraPosition) {
                searchNearbyPoi(position)
            }
        })
        val uiSettings = mMap!!.uiSettings
        uiSettings.isCompassEnabled = false
        uiSettings.isMyLocationButtonEnabled = false
        uiSettings.isZoomControlsEnabled = false

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_LOCATION_PERMISSION)
    }

    private fun searchNearbyPoi(position: CameraPosition) {
        val lm = supportLoaderManager
        val args = Bundle()
        args.putParcelable(Constants.POSITION, position.target)
        args.putParcelable(Constants.BOUNDS, mMap!!.projection.visibleRegion.latLngBounds)
        if (mLoaderInitialized) {
            lm.restartLoader(0, args, this)
        } else {
            lm.initLoader(0, args, this)
            mLoaderInitialized = true
        }
    }

    override fun onDestroy() {
        mMapView!!.onDestroy()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView!!.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        mMapView!!.onLowMemory()
        super.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val criteria = Criteria()
                    mLocationManager!!.requestLocationUpdates(5000L, 0f, criteria, this,
                            Looper.getMainLooper())
                    mMap!!.isMyLocationEnabled = true
                } else {
                    mMap!!.isMyLocationEnabled = false
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPause() {
        mMapView!!.onPause()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager!!.removeUpdates(this)
        }
        super.onPause()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        mMapView = findViewById(R.id.map_view) as MapView?
        mPlacesList = findViewById(R.id.places_list) as RecyclerView?
    }


    override fun onLocationChanged(location: Location) {
        if (mOnLocationChangedListener != null) {
            mOnLocationChangedListener!!.onLocationChanged(location)
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<PoiResult> {
        val position = args.getParcelable<LatLng>(Constants.POSITION)
        val region = args.getParcelable<LatLngBounds>(Constants.BOUNDS)
        return NearByPoiLoader(this, position, region)
    }

    override fun onLoadFinished(loader: Loader<PoiResult>, data: PoiResult?) {
        val items = ArrayList<Any>()
        items.add(CurrentLocation())
        if (data != null) {
            items.addAll(data.pois)
        }
        mAdapter!!.items = items
        mAdapter!!.notifyDataSetChanged()
    }

    override fun onLoaderReset(loader: Loader<PoiResult>) {
        mAdapter!!.items = null
        mAdapter!!.notifyDataSetChanged()
    }

    private fun notifyCurrentLocationClick() {
        val myLocation = mMap!!.myLocation ?: return
        val latLng = LatLng(myLocation.latitude, myLocation.longitude)
        showMarker(latLng, "My location", true)
    }

    private fun notifyPoiItemClick(item: PoiItem) {
        val latLng = AMapModelUtils.toLatLng(item.latLonPoint)
        showMarker(latLng, item.title, true)
    }

    private fun showMarker(latLng: LatLng?, name: String, center: Boolean) {
        if (mMarker != null) {
            mMarker!!.remove()
            mMarker!!.destroy()
            mMarker = null
        }
        if (latLng == null) return
        val options = MarkerOptions()
        options.draggable(false)
        options.position(latLng)
        options.title(name)
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pin))
        options.anchor(0.5f, 1f)
        mMarker = mMap!!.addMarker(options)
        if (center) {
            mMap!!.animateCamera(CameraUpdateFactory.changeLatLng(latLng))
        }
    }

    class NearByPoiLoader(context: Context, private val mLatLng: LatLng?, private val mBounds: LatLngBounds?) : AsyncTaskLoader<PoiResult>(context) {

        override fun onStartLoading() {
            forceLoad()
        }

        override fun loadInBackground(): PoiResult? {
            if (mLatLng == null || mBounds == null) return null
            try {
                val geocodeSearch = GeocodeSearch(context)
                val address = geocodeSearch.getFromLocation(RegeocodeQuery(
                        LatLonPoint(mLatLng.latitude, mLatLng.longitude), 0f, GeocodeSearch.GPS))
                val poiSearch = PoiSearch(context, PoiSearch.Query("", "景点",
                        address.adCode))
                poiSearch.bound = AMapModelUtils.toSearchBound(mBounds)
                return poiSearch.searchPOI()
            } catch (e: AMapException) {
                return null
            } catch (e: IllegalArgumentException) {
                return null
            }

        }

    }

    class LocationAdapter(private val activity: LocationPickerActivity) : ListDelegationAdapter<List<Any>>() {

        init {
            // DelegatesManager is a protected Field in ListDelegationAdapter
            delegatesManager.addDelegate(CurrentLocationDelegate(activity, this, activity.layoutInflater, 0))
            delegatesManager.addDelegate(PoiItemDelegate(activity, this, activity.layoutInflater, 1))
        }

        fun notifyItemClick(position: Int) {
            val item = items[position]
            if (item is PoiItem) {
                activity.notifyPoiItemClick(item)
            } else if (item is CurrentLocation) {
                activity.notifyCurrentLocationClick()
            }
        }
    }

    private class PoiItemDelegate(context: Context, private val adapter: LocationAdapter,
                                  private val inflater: LayoutInflater, private val viewType: Int) : AdapterDelegate<List<Any>> {

        override fun getItemViewType(): Int {
            return viewType
        }

        override fun isForViewType(items: List<Any>, position: Int): Boolean {
            return items[position] is PoiItem
        }

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            return PlaceViewHolder(adapter, inflater.inflate(R.layout.list_item_location_picker_place, parent, false))
        }

        override fun onBindViewHolder(items: List<Any>, position: Int, holder: RecyclerView.ViewHolder) {
            val poiItem = items[position] as PoiItem
            (holder as PlaceViewHolder).display(R.drawable.ic_place_pin, poiItem.title)
        }


    }

    internal class CurrentLocation

    private class CurrentLocationDelegate(private val context: Context, private val adapter: LocationAdapter,
                                          private val inflater: LayoutInflater, private val viewType: Int) : AdapterDelegate<List<Any>> {

        override fun getItemViewType(): Int {
            return viewType
        }

        override fun isForViewType(items: List<Any>, position: Int): Boolean {
            return items[position] is CurrentLocation
        }

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            return PlaceViewHolder(adapter, inflater.inflate(R.layout.list_item_location_picker_place, parent, false))
        }

        override fun onBindViewHolder(items: List<Any>, position: Int, holder: RecyclerView.ViewHolder) {
            (holder as PlaceViewHolder).display(R.drawable.ic_place_current_location, context.getString(R.string.my_current_location))
        }

    }

    private class PlaceViewHolder(private val adapter: LocationAdapter, view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val iconView: ImageView
        private val titleView: TextView

        init {
            itemView.setOnClickListener(this)
            iconView = itemView.findViewById(android.R.id.icon) as ImageView
            titleView = itemView.findViewById(android.R.id.title) as TextView
        }

        fun display(icon: Int, title: CharSequence) {
            iconView.setImageResource(icon)
            titleView.text = title
        }

        override fun onClick(v: View) {
            adapter.notifyItemClick(layoutPosition)
        }
    }

    companion object {

        private val REQUEST_LOCATION_PERMISSION = 101
    }
}
