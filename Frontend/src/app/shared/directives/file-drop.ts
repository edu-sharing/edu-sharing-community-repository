import {
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
    inject,
} from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

export interface Options {
    readAs?: string;
}

/**
 * A folder dropped via drag & drop, including all its (nested) files and sub folders.
 *
 * The root folder of a drop is virtual (its `name` is empty) and contains the dropped top-level
 * files and folders.
 */
export interface DroppedFolder {
    name: string;
    files: File[];
    folders: DroppedFolder[];
}

@Directive({
    selector: '[esFileDrop]',
    standalone: false,
})
export class FileDropDirective implements OnInit, OnDestroy {
    private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private ngZone = inject(NgZone);

    @Input() options: Options;
    /**
     * catch drag/drop of whole window
     */
    @Input() window = false;
    /**
     * Allow dropping folders. If a drop contains at least one folder, `folderDrop` is emitted
     * (instead of `fileDrop`) with the complete tree of the dropped files and folders.
     */
    @Input() allowFolders = false;

    @Output() fileOver: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() fileDrop: EventEmitter<FileList> = new EventEmitter<FileList>();
    @Output() folderDrop: EventEmitter<DroppedFolder> = new EventEmitter<DroppedFolder>();

    /**
     * Element the drag is currently over, or null while no drag is in progress.
     *
     * Browsers deliver neither a balanced nor an ordered sequence of `dragenter` and `dragleave`:
     * the enter of the element being entered arrives before the leave of the one being left, and a
     * leave is dropped entirely when its element gets covered mid-drag — which is what the upload
     * overlay does to whatever sits beneath it. Counting the events therefore cannot detect the end
     * of a drag, because a single lost leave puts the count permanently out of reach of zero.
     *
     * Remembering the current element instead makes both harmless: a leave for anything other than
     * the current element belongs to one already superseded and is ignored, a lost leave is never
     * missed because nothing waits for it, and the leave that does match marks the actual end of
     * the drag.
     */
    private currentTarget: EventTarget = null;
    private destroyed = new Subject<void>();
    private fileOverSubject = new BehaviorSubject(false);

    ngOnInit(): void {
        this.registerEvents();
        this.registerOutputs();
    }

    ngOnDestroy(): void {
        this.leaveDragOver();
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerEvents() {
        const target = this.getTarget();
        // All event handlers run outside Angular's zone. We only enter the zone again when emitting
        // on Outputs.
        this.ngZone.runOutsideAngular(() => {
            addEventListenerUntil(target, 'dragenter', this.onDragEnter, this.destroyed);
            addEventListenerUntil(target, 'dragover', this.onDragOver, this.destroyed);
            addEventListenerUntil(target, 'dragleave', this.onDragLeave, this.destroyed);
            addEventListenerUntil(target, 'drop', this.onDrop, this.destroyed);
        });
    }

    private registerOutputs() {
        // Avoid unnecessary change-detection cycles by only emitting on distinct values.
        this.fileOverSubject.pipe(distinctUntilChanged()).subscribe((value) => {
            this.ngZone.run(() => {
                this.fileOver.emit(value);
            });
        });
    }

    private getTarget(): EventTarget {
        if (this.window) {
            return window;
        } else {
            return this.elementRef.nativeElement;
        }
    }

    private onDragEnter = (event: DragEvent) => {
        const transfer = this.getDataTransfer(event);
        if (this.haveFiles(transfer.types)) {
            this.preventAndStop(event);
            transfer.dropEffect = 'copy';
            this.enterDragOver(event.target);
        }
    };

    private onDragOver = (event: DragEvent) => {
        const transfer = this.getDataTransfer(event);
        if (this.haveFiles(transfer.types)) {
            // If we don't call `preventDefault` on dragover events, we won't get notified of drop
            // events.
            this.preventAndStop(event);
            transfer.dropEffect = 'copy';
            // Should a `dragenter` ever be skipped, dragover carries the more recent truth about
            // which element the drag sits on, and the drag-over state is re-asserted along with it.
            this.enterDragOver(event.target);
        }
    };

    private onDragLeave = (event: DragEvent) => {
        if (event.target === this.currentTarget) {
            this.leaveDragOver();
        }
    };

    private onDrop = (event: DragEvent) => {
        const transfer = this.getDataTransfer(event);
        this.leaveDragOver();
        const hasFiles = this.haveFiles(transfer.types) && transfer.files.length;
        if (hasFiles) {
            this.preventAndStop(event);
            // entries are only accessible synchronously during the drop event
            const entries = this.allowFolders ? this.getEntries(transfer) : [];
            if (entries.some((entry) => entry.isDirectory)) {
                void this.readFolder(entries);
            } else {
                this.readFile(transfer.files);
            }
        }
    };

    private getEntries(transfer: DataTransfer): FileSystemEntry[] {
        return Array.from(transfer.items ?? [])
            .filter((item) => item.kind === 'file')
            .map((item) => item.webkitGetAsEntry?.())
            .filter((entry) => !!entry);
    }

    private async readFolder(entries: FileSystemEntry[]): Promise<void> {
        try {
            const folder = await readEntries('', entries);
            this.ngZone.run(() => {
                this.folderDrop.emit(folder);
            });
        } catch (e) {
            console.error('Could not read dropped folder', e);
        }
    }

    /**
     * Marks `target` as the element the drag sits on and enters the drag-over state.
     */
    private enterDragOver(target: EventTarget): void {
        this.currentTarget = target;
        this.emitFileOver(true);
    }

    /**
     * Leaves the drag-over state and forgets the current element.
     */
    private leaveDragOver(): void {
        this.currentTarget = null;
        this.emitFileOver(false);
    }

    private readFile(file: FileList): void {
        const strategy = this.pickStrategy();

        if (!strategy) {
            this.emitFileDrop(file);
        } else {
            /*
      // XXX Waiting for angular/zone.js#358
      const method = `readAs${strategy}`;

      FileAPI[method](file, (event) => {
        if (event.type === 'load') {
          this.emitFileDrop(event.result);
        } else if (event.type === 'error') {
          throw new Error(`Couldn't read file '${file.name}'`);
        }
      });
      */
        }
    }

    private emitFileOver(isOver: boolean): void {
        this.fileOverSubject.next(isOver);
    }

    private emitFileDrop(file: FileList): void {
        this.ngZone.run(() => {
            this.fileDrop.emit(file);
        });
    }

    private pickStrategy(): string | void {
        if (!this.options) {
            return;
        }

        if (this.hasStrategy(this.options.readAs)) {
            return this.options.readAs;
        }
    }

    private hasStrategy(type: string): boolean {
        return ['DataURL', 'BinaryString', 'ArrayBuffer', 'Text'].indexOf(type) !== -1;
    }

    private getDataTransfer(event: any | any): DataTransfer {
        return event.dataTransfer ? event.dataTransfer : event.originalEvent.dataTransfer;
    }

    private preventAndStop(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
    }

    private haveFiles(types: any): boolean {
        if (!types) {
            return false;
        }

        if (types.indexOf) {
            return types.indexOf('text/uri-list') === -1 && types.indexOf('Files') !== -1;
        }

        if (types.contains) {
            return types.contains('Files');
        }

        return false;
    }
}

function addEventListenerUntil<T extends Event>(
    target: EventTarget,
    eventName: string,
    callback: (event: T) => void,
    until: Observable<void>,
) {
    target.addEventListener(eventName, callback);
    until.subscribe(() => target.removeEventListener(eventName, callback));
}

async function readEntries(name: string, entries: FileSystemEntry[]): Promise<DroppedFolder> {
    const folder: DroppedFolder = { name, files: [], folders: [] };
    for (const entry of entries) {
        if (entry.isFile) {
            folder.files.push(
                await new Promise<File>((resolve, reject) =>
                    (entry as FileSystemFileEntry).file(resolve, reject),
                ),
            );
        } else if (entry.isDirectory) {
            const children = await readDirectory(entry as FileSystemDirectoryEntry);
            folder.folders.push(await readEntries(entry.name, children));
        }
    }
    return folder;
}

async function readDirectory(directory: FileSystemDirectoryEntry): Promise<FileSystemEntry[]> {
    const reader = directory.createReader();
    const result: FileSystemEntry[] = [];
    // readEntries returns the children in batches, an empty batch marks the end
    while (true) {
        const batch = await new Promise<FileSystemEntry[]>((resolve, reject) =>
            reader.readEntries(resolve, reject),
        );
        if (!batch.length) {
            return result;
        }
        result.push(...batch);
    }
}
