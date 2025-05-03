/**
 * Solar Panel ROI Calculator Interface
 *
 * This interface defines the parameters needed for calculating the return on investment
 * for a solar panel installation with battery storage.
 */

export type CardinalDirection =
  | "north"
  | "north-east"
  | "north-west"
  | "south"
  | "south-east"
  | "south-west"
  | "east"
  | "west";

export interface SolarRoiCalculatorParams {
  /**
   * The direction the solar panels are facing
   */
  solarPanelDirection: CardinalDirection;

  /**
   * Whether the homeowner currently has or plans to get an electric vehicle
   */
  haveOrWillGetEv: boolean;

  /**
   * Whether someone is typically at home during standard work hours (9am-5pm)
   */
  homeOccupancyDuringWorkHours: boolean;

  /**
   * Whether financing is needed for the solar installation
   */
  needFinance: boolean;

  /**
   * The size of the battery storage in kWh
   */
  batterySize: number;

  /**
   * Annual electricity usage in kWh
   */
  usage: number;

  /**
   * Size of the solar panel system in kW
   */
  solarSize: number;
}

/**
 * Example usage:
 *
 * const params: SolarRoiCalculatorParams = {
 *   solarPanelDirection: "south",
 *   haveOrWillGetEv: true,
 *   homeOccupancyDuringWorkHours: false,
 *   needFinance: true,
 *   batterySize: 13.5,
 *   usage: 5000,
 *   solarSize: 6.6
 * };
 */
