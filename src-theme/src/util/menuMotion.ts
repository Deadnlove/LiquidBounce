// Shared with the ESP smoothstep curve: 3t² − 2t³.
export const menuEase = (progress: number): number => {
    const t = Math.min(1, Math.max(0, progress));
    return t * t * (3 - 2 * t);
};
export const menuDuration = 280;
