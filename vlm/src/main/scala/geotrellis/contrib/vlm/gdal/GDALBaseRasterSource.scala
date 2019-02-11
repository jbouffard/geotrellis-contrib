/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.contrib.vlm.gdal

import geotrellis.contrib.vlm._
import geotrellis.gdal._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.reproject.Reproject
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector._

import org.gdal.gdal.Dataset

import java.net.MalformedURLException

trait GDALBaseRasterSource extends RasterSource {
  val vsiPath: String = if (VSIPath.isVSIFormatted(uri)) uri else try {
    VSIPath(uri).vsiPath
  } catch {
    case _: Throwable =>
      throw new MalformedURLException(
        s"Invalid URI passed into the GDALRasterSource constructor: ${uri}." +
          s"Check geotrellis.contrib.vlm.gdal.VSIPath constrains, " +
          s"or pass VSI formatted String into the GDALRasterSource constructor manually."
      )
  }

  /** options to override some values on transformation steps, should be used carefully as these params can change the behaviour significantly */
  private[gdal] val options: GDALWarpOptions

  /** options from previous transformation steps */
  private[gdal] val baseWarpList: List[GDALWarpOptions]

  /** current transformation options */
  private[gdal] val warpOptions: GDALWarpOptions

  /** the list of transformation options including the current one */
  lazy private[gdal] val warpList: List[GDALWarpOptions] = baseWarpList :+ warpOptions

  // generate a vrt before the current options application
  @transient private[vlm] lazy val fromBaseWarpList: Dataset = {
    val (ds, history) = GDAL.fromGDALWarpOptionsH(uri, baseWarpList)
    parentDatasets ++= history
    ds
  }
  // current dataset
  @transient private[vlm] lazy val dataset: Dataset = {
    val (ds, history) = GDAL.fromGDALWarpOptionsH(uri, warpList, fromBaseWarpList)
    parentDatasets ++= history
    ds
  }

  lazy val bandCount: Int = dataset.getRasterCount

  lazy val crs: CRS = dataset.crs.getOrElse(CRS.fromEpsgCode(4326))

  protected lazy val reader: GDALReader = GDALReader(dataset)

  // noDataValue from the previous step
  lazy val noDataValue: Option[Double] = baseDataset.getNoDataValue

  lazy val cellType: CellType = dataset.cellType

  lazy val rasterExtent: RasterExtent = dataset.rasterExtent

  /** Resolutions of available overviews in GDAL Dataset
    *
    * These resolutions could represent actual overview as seen in source file
    * or overviews of VRT that was created as result of resample operations.
    */
  lazy val resolutions: List[RasterExtent] = {
    val band = dataset.GetRasterBand(1)
    rasterExtent :: (0 until band.GetOverviewCount).toList.map { idx =>
      val ovr = band.GetOverview(idx)
      RasterExtent(extent, cols = ovr.getXSize, rows = ovr.getYSize)
    }
  }

  override def readBounds(bounds: Traversable[GridBounds], bands: Seq[Int]): Iterator[Raster[MultibandTile]] = {
    bounds
      .toIterator
      .flatMap { gb => gridBounds.intersection(gb) }
      .map { gb =>
        val tile = reader.read(gb, bands = bands)
        val extent = rasterExtent.extentFor(gb)
        Raster(tile, extent)
      }
  }

  def reproject(targetCRS: CRS, reprojectOptions: Reproject.Options, strategy: OverviewStrategy): RasterSource =
    GDALReprojectRasterSource(uri, reprojectOptions, strategy, options.reproject(rasterExtent, crs, targetCRS, reprojectOptions))

  def resample(resampleGrid: ResampleGrid, method: ResampleMethod, strategy: OverviewStrategy): RasterSource = {
    GDALResampleRasterSource(uri, resampleGrid, method, strategy, options.resample(rasterExtent.toGridExtent, resampleGrid))
  }

  /** Converts the contents of the GDALRasterSource to the target [[CellType]].
   *
   *  Note:
   *
   *  GDAL handles Byte data differently than GeoTrellis. Unlike GeoTrellis,
   *  GDAL treats all Byte data as Unsigned Bytes. Thus, the output from
   *  converting to a Signed Byte CellType can result in unexpected results.
   *  When given values to convert to Byte, GDAL takes the following steps:
   *
   *  1. Checks to see if the values falls in [0, 255].
   *  2. If the value falls outside of that range, it'll clamp it so that
   *  it falls within it. For example: -1 would become 0 and 275 would turn
   *  into 255.
   *  3. If the value falls within that range and is a floating point, then
   *  GDAL will round it up. For example: 122.492 would become 122 and 64.1
   *  would become 64.
   *
   *  Thus, it is recommended that one avoids converting to Byte without first
   *  ensuring that no data will be lost.
   *
   *  Note:
   *
   *  It is not currently possible to convet to the [[BitCellType]] using GDAL.
   *  @group convert
   */
  def convert(cellType: CellType, strategy: OverviewStrategy): RasterSource =
    GDALConvertedRasterSource(uri, cellType, strategy, options)

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = rasterExtent.gridBoundsFor(extent, clamp = false)
    read(bounds, bands)
  }

  def read(bounds: GridBounds, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val it = readBounds(List(bounds).flatMap(_.intersection(this)), bands)
    if (it.hasNext) Some(it.next) else None
  }

  override def readExtents(extents: Traversable[Extent]): Iterator[Raster[MultibandTile]] = {
    // TODO: clamp = true when we have PaddedTile ?
    val bounds = extents.map(rasterExtent.gridBoundsFor(_, clamp = false))
    readBounds(bounds, 0 until bandCount)
  }

  override def close = dataset.delete
}
