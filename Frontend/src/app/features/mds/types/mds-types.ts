/**
 * - `nodes`:
 *   - Supports bulk.
 *   - Returns only changed values.
 * - `search`:
 *   - No bulk.
 *   - All values returned.
 *   - Trees sub-children are auto-selected if root is selected.
 *   - Required errors and -warnings are disabled.
 * - `form`:
 *   - No bulk.
 *   - All values returned.
 * - `inline`
 *   - No bulk
 *   - Editing individual values on demand
 *   - default apperance is read only
 * - `viewer`
 *   - No editing
 *   - Read only
 *   - Triggered via mds-viewer
 */
export type EditorMode = 'nodes' | 'search' | 'form' | 'inline' | 'viewer';
